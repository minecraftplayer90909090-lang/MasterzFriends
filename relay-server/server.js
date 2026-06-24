import 'dotenv/config';
import express from 'express';
import { WebSocketServer } from 'ws';
import { createServer } from 'http';
import { v4 as uuidv4 } from 'uuid';
import cors from 'cors';
import fs from 'fs';
import path from 'path';

// ── Simple JSON Storage (no native modules needed) ────────────────────────────
const DB_FILE = './db.json';

function loadDB() {
  try {
    if (fs.existsSync(DB_FILE)) return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch {}
  return { users: {}, friends: {}, sessions: {}, messages: [] };
}

function saveDB(db) {
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
}

let db = loadDB();

// ── Discord OAuth ─────────────────────────────────────────────────────────────
const { DISCORD_CLIENT_ID, DISCORD_CLIENT_SECRET, DISCORD_REDIRECT_URI, PORT = 3000 } = process.env;

function getDiscordAuthUrl(code) {
  const params = new URLSearchParams({
    client_id: DISCORD_CLIENT_ID,
    redirect_uri: DISCORD_REDIRECT_URI,
    response_type: 'code',
    scope: 'identify',
    state: code,
  });
  return `https://discord.com/oauth2/authorize?${params}`;
}

async function exchangeCode(authCode) {
  const res = await fetch('https://discord.com/api/oauth2/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: DISCORD_CLIENT_ID,
      client_secret: DISCORD_CLIENT_SECRET,
      grant_type: 'authorization_code',
      code: authCode,
      redirect_uri: DISCORD_REDIRECT_URI,
    }),
  });
  return res.json();
}

async function getDiscordUser(token) {
  const res = await fetch('https://discord.com/api/users/@me', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

// ── Express ───────────────────────────────────────────────────────────────────
const app = express();
app.use(cors());
app.use(express.json());
const server = createServer(app);

app.get('/', (req, res) => res.json({ status: '🎮 Masterz Friends Relay Online!' }));

app.get('/auth/url', (req, res) => {
  const code = uuidv4();
  db.sessions[code] = { code, token: null, discord_id: null, username: null };
  saveDB(db);
  res.json({ url: getDiscordAuthUrl(code), code });
});

app.get('/auth/callback', async (req, res) => {
  const { code: authCode, state: sessionCode } = req.query;
  try {
    const tokenData = await exchangeCode(authCode);
    const user = await getDiscordUser(tokenData.access_token);
    db.users[user.id] = { discord_id: user.id, username: user.username, avatar: user.avatar || '' };
    const internalToken = uuidv4();
    db.sessions[sessionCode] = { token: internalToken, discord_id: user.id, username: user.username };
    saveDB(db);
    res.send(`<html><body style="background:#0D1A2E;color:#00E5FF;font-family:sans-serif;text-align:center;padding-top:80px">
      <h2>✅ Logged in as <b>${user.username}</b>!</h2>
      <p style="color:#aaa">Tab band karo, Minecraft mein wapas jao.</p>
    </body></html>`);
  } catch (e) {
    res.status(500).send('Auth failed: ' + e.message);
  }
});

app.get('/auth/token', (req, res) => {
  const session = db.sessions[req.query.code];
  if (!session || !session.token) return res.status(202).json({ pending: true });
  res.json({ token: session.token, discord_id: session.discord_id, username: session.username });
});

// ── WebSocket ─────────────────────────────────────────────────────────────────
const wss = new WebSocketServer({ server });
const clients = new Map(); // discord_id -> ws

function getFriends(userId) {
  const f = db.friends[userId] || [];
  return f.filter(x => x.status === 'accepted').map(x => ({
    ...db.users[x.id],
    status: clients.has(x.id) ? 'online' : 'offline'
  }));
}

function broadcastStatus(discordId, status) {
  const friends = getFriends(discordId);
  for (const f of friends) {
    const ws = clients.get(f.discord_id);
    if (ws?.readyState === 1) {
      ws.send(JSON.stringify({ type: 'status_update', discord_id: discordId, status }));
    }
  }
}

wss.on('connection', (ws) => {
  let myId = null;

  ws.on('message', (raw) => {
    let pkt;
    try { pkt = JSON.parse(raw.toString()); } catch { return; }

    if (pkt.type === 'auth') {
      const session = Object.values(db.sessions).find(s => s.token === pkt.token);
      if (!session) { ws.send(JSON.stringify({ type: 'error', message: 'Invalid token' })); return; }
      myId = session.discord_id;
      clients.set(myId, ws);
      ws.send(JSON.stringify({ type: 'auth_ok', discord_id: myId, username: session.username, friends: getFriends(myId) }));
      broadcastStatus(myId, 'online');
      return;
    }

    if (!myId) return;
    const me = db.users[myId];

    if (pkt.type === 'dm') {
      const targetWs = clients.get(pkt.to);
      if (targetWs?.readyState === 1) {
        targetWs.send(JSON.stringify({ type: 'dm', from_id: myId, from_username: me.username, message: pkt.message }));
      }
    }

    if (pkt.type === 'friend_request') {
      if (!db.friends[myId]) db.friends[myId] = [];
      if (!db.friends[pkt.to]) db.friends[pkt.to] = [];
      if (!db.friends[myId].find(f => f.id === pkt.to)) {
        db.friends[myId].push({ id: pkt.to, status: 'pending' });
        db.friends[pkt.to].push({ id: myId, status: 'pending' });
        saveDB(db);
      }
      const targetWs = clients.get(pkt.to);
      if (targetWs?.readyState === 1) {
        targetWs.send(JSON.stringify({ type: 'friend_request', from_id: myId, from_username: me.username }));
      }
    }

    if (pkt.type === 'friend_accept') {
      const myEntry = db.friends[myId]?.find(f => f.id === pkt.to);
      const theirEntry = db.friends[pkt.to]?.find(f => f.id === myId);
      if (myEntry) myEntry.status = 'accepted';
      if (theirEntry) theirEntry.status = 'accepted';
      saveDB(db);
      const targetWs = clients.get(pkt.to);
      if (targetWs?.readyState === 1) {
        targetWs.send(JSON.stringify({ type: 'friend_accepted', from_id: myId, from_username: me.username }));
      }
    }

    if (pkt.type === 'friend_remove') {
      if (db.friends[myId]) db.friends[myId] = db.friends[myId].filter(f => f.id !== pkt.to);
      if (db.friends[pkt.to]) db.friends[pkt.to] = db.friends[pkt.to].filter(f => f.id !== myId);
      saveDB(db);
    }
  });

  ws.on('close', () => {
    if (myId) { clients.delete(myId); broadcastStatus(myId, 'offline'); }
  });
});

server.listen(PORT, () => {
  console.log(`\n🎮 Masterz Friends Relay running on port ${PORT}`);
  console.log(`   http://localhost:${PORT}`);
});
