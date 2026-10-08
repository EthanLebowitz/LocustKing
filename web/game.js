// Web port of LocustKing (Java: Main, World, Boid/Locust/KingBoid, Tile, Pair).
// Mechanics and constants mirror the Java source.
"use strict";

const WIDTH = 1024, HEIGHT = 768, FPS = 60;
const TILE = 300;               // tile sprites are 300x300
const WORLD_DIM = 100;          // world is 100x100 tiles
const ZOOM = 0.5;               // world-to-screen scale (sprites for king/locusts/UI stay full size)

// ---- Pair ---------------------------------------------------------------
class Pair {
  constructor(x, y) { this.x = x; this.y = y; }
  add(o) { return new Pair(this.x + o.x, this.y + o.y); }
  times(v) { return new Pair(this.x * v, this.y * v); }
  normalizeVector() {
    const m = Math.sqrt(this.x * this.x + this.y * this.y);
    return new Pair(this.x / m, this.y / m);
  }
}
const dist = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);

// ---- Sprites ------------------------------------------------------------
const SPRITE_NAMES = ["live1", "live2", "live3", "live4", "dead1", "dead2", "mtn",
                      "king_crown_small", "locust_swarm"];
const sprites = {};
function loadSprites() {
  return Promise.all(SPRITE_NAMES.map(n => new Promise((res, rej) => {
    const img = new Image();
    img.onload = () => { sprites[n] = img; res(); };
    img.onerror = () => rej(new Error("failed to load sprite " + n));
    img.src = "sprites/" + n + ".png";
  })));
}

// ---- Tiles --------------------------------------------------------------
class Tile {
  constructor(x, y, life, world) {
    this.position = new Pair(x, y);
    this.life = life;
    this.alive = life > 0;
    this.world = world;
    this.isMountain = false;
    this.sprite = "live4";
    this.updateSprite();
  }
  updateSprite() {
    if (!this.alive) return;
    if (this.life <= 0) this.makeDead();
    else if (this.life >= 75) this.sprite = "live4";
    else if (this.life >= 50) this.sprite = "live3";
    else if (this.life >= 25) this.sprite = "live2";
    else this.sprite = "live1";
  }
  // When life reaches zero the tile dies and 0-2 locusts join the swarm near the king.
  makeDead() {
    this.life = 0;
    this.alive = false;
    this.sprite = Math.random() > 0.5 ? "dead1" : "dead2";
    const n = Math.floor(Math.random() * 3);
    const w = this.world;
    for (let i = 0; i < n; i++) {
      w.boids.push(new Locust((Math.random() - .5) * 400 + w.kingBoid.position.x,
                              (Math.random() - .5) * 400 + w.kingBoid.position.y, w));
    }
  }
  draw(ctx, center) {
    const cx = this.position.x * TILE, cy = this.position.y * TILE;
    if (cx < center.x - WIDTH / 2 / ZOOM - TILE || cx > center.x + WIDTH / 2 / ZOOM + TILE) return;
    if (cy < center.y - HEIGHT / 2 / ZOOM - TILE || cy > center.y + HEIGHT / 2 / ZOOM + TILE) return;
    const d = World.toDisplayCoords(new Pair(cx, cy), center);
    const s = Math.ceil(TILE * ZOOM) + 1;   // +1 hides seams
    ctx.drawImage(sprites[this.sprite], Math.floor(d.x), Math.floor(d.y), s, s);
  }
}
class MountainTile extends Tile {
  constructor(x, y, life, world) {
    super(x, y, life, world);
    this.sprite = "mtn";
    this.isMountain = true;
  }
  updateSprite() {}
}

// ---- Boids --------------------------------------------------------------
class Boid {
  constructor(x, y, world) {
    this.position = new Pair(x, y);
    this.velocity = new Pair((Math.random() - .5) * 10, (Math.random() - .5) * 10);
    this.world = world;
    this.boidWidth = 0;
    this.boidHeight = 0;
  }
  getTile(map) {
    const tx = Math.floor((this.position.x + .5 * this.boidWidth) / TILE);
    const ty = Math.floor((this.position.y + .5 * this.boidHeight) / TILE);
    if (tx >= 0 && tx < map[0].length && ty >= 0 && ty < map.length) return map[ty][tx];
    return map[0][0];
  }
  reduceTileLife(tile) {
    if (tile.alive && tile.life > 0) tile.life -= .1;
    tile.updateSprite();
  }
}

class Locust extends Boid {
  constructor(x, y, world) {
    super(x, y, world);
    world.numBoids++;
    if (Locust.life <= 0) Locust.life = 100;
  }
  update(time, map, boids, king) {
    this.flock(boids, king);
    this.position = this.position.add(this.velocity.times(time));
    const tile = this.getTile(map);
    this.reduceTileLife(tile);
    this.updateBoidLife(tile);
  }
  updateBoidLife(tile) {
    if (tile.alive && Locust.life < 100) Locust.life += Locust.healRate;
    else if (!tile.alive && Locust.life > 0) Locust.life -= Locust.decayRate;
    if (Locust.life <= 0) this.die();
  }
  die() {
    this.world.numBoids--;
    this.world.boids.pop();   // the last locust added drops out of the swarm
    if (this.world.numBoids > 0) Locust.life = 100;
  }
  // Separation from nearby locusts in front of this one (alignment/cohesion weights are 0 in the original).
  neighborhood(me, boids) {
    const vel = me.velocity.normalizeVector();
    const separate = new Pair(0, 0);
    const ahead = me.position.add(me.velocity.normalizeVector().times(25));
    let count = 0;
    for (const o of boids) {
      if (o === me) continue;
      if (dist(me.position, o.position) < 25 && dist(ahead, o.position) < 25) {
        count++;
        separate.x += o.position.x - me.position.x;
        separate.y += o.position.y - me.position.y;
      }
    }
    if (count === 0) return vel;
    const s = new Pair(-separate.x / count, -separate.y / count).normalizeVector();
    vel.x += s.x * 0.7;
    vel.y += s.y * 0.7;
    return vel.normalizeVector().times(50);
  }
  flock(boids, king) {
    this.velocity = this.neighborhood(this, boids).add(this.follow(king)).times(4);
  }
  follow(king) {
    const to = new Pair(king.position.x - this.position.x, king.position.y - this.position.y).normalizeVector();
    let target = Math.atan(to.y / to.x);
    if (to.x < 0) target += Math.PI;
    else if (to.x > 0 && to.y < 0) target += Math.PI * 2;
    let cur = Math.atan(this.velocity.y / this.velocity.x);
    if (this.velocity.x < 0) cur += Math.PI;
    else if (this.velocity.x > 0 && this.velocity.y < 0) cur += Math.PI * 2;
    let change = (Math.PI / 180) * 5;
    const big = Math.abs(cur - target) > Math.PI;
    if (cur > target) change = big ? change : -change;
    else if (cur < target) change = big ? -change : change;
    if (Math.hypot(king.position.x - this.position.x, king.position.y - this.position.y) < 50) {
      change = -change * 2;   // too close: steer away
    }
    const a = cur + change;
    return new Pair(Math.cos(a), Math.sin(a)).times(100);
  }
  draw(ctx, world) {
    const p = World.toDisplayCoords(this.position, world.kingBoid.position);
    ctx.fillStyle = "rgb(172,21,0)";
    ctx.beginPath();
    ctx.arc(p.x + 7.5, p.y + 7.5, 7.5, 0, Math.PI * 2);
    ctx.fill();
  }
}
Locust.life = 100;       // shared "swarm health"
Locust.decayRate = .1;
Locust.healRate = .9;

class KingBoid extends Boid {
  constructor(x, y, world) {
    super(x, y, world);
    this.boidWidth = sprites.king_crown_small.width;
    this.boidHeight = sprites.king_crown_small.height;
  }
  update(time) {
    const slow = this.getTile(this.world.map).isMountain ? 2.5 : 1;
    this.position = this.position.add(this.velocity.times(time / slow));
    this.contain();
  }
  follow(target) {   // target is in canvas coords; the king flies toward it from screen center
    this.velocity = new Pair(target.x - WIDTH / 2, target.y - HEIGHT / 2);
  }
  contain() {
    const max = this.world.dim * TILE;
    this.position.x = Math.min(Math.max(this.position.x, 0), max);
    this.position.y = Math.min(Math.max(this.position.y, 0), max);
  }
  draw(ctx) {
    const v = this.velocity.normalizeVector();
    ctx.save();
    ctx.translate(WIDTH / 2, HEIGHT / 2);
    ctx.rotate((isNaN(v.x) ? 0 : Math.atan2(v.y, v.x)) + Math.PI / 2);
    ctx.drawImage(sprites.king_crown_small, -this.boidWidth / 2, -this.boidHeight / 2);
    ctx.restore();
  }
}

// ---- World --------------------------------------------------------------
class World {
  constructor(dim, game) {
    this.dim = dim;
    this.game = game;
    this.numBoids = 0;
    this.boids = [];
    Locust.life = 100;
    this.kingBoid = new KingBoid(2000, 2000, this);
    this.map = this.generateMap(dim);
    this.boids.push(new Locust((Math.random() - .5) * 400 + 2000, (Math.random() - .5) * 400 + 2000, this));
  }
  // Seed random mountain/live points, then every tile takes the type of its nearest seed.
  generateMap(dim) {
    const mtn = [], live = [];
    for (let i = 0; i < dim; i++) for (let j = 0; j < dim; j++) {
      const r = Math.random();
      if (r < 0.1) mtn.push([j, i]);
      else if (r < 0.3) live.push([j, i]);
    }
    const nearest = (seeds, x, y) => {
      let best = Infinity;
      for (const [sx, sy] of seeds) {
        const d = (x - sx) * (x - sx) + (y - sy) * (y - sy);
        if (d < best) best = d;
      }
      return best;
    };
    const map = [];
    for (let i = 0; i < dim; i++) {
      map.push([]);
      for (let j = 0; j < dim; j++) {
        const border = i === 0 || j === 0 || i === dim - 1 || j === dim - 1;
        if (border || nearest(mtn, j, i) < nearest(live, j, i)) map[i].push(new MountainTile(j, i, 0, this));
        else map[i].push(new Tile(j, i, Math.random() * 100 + .1, this));
      }
    }
    return map;
  }
  getPercentAlive() {
    let alive = 0, dead = 0;
    for (const row of this.map) for (const t of row) {
      if (t.alive) alive++;
      else if (!t.isMountain) dead++;
    }
    if (alive === 0) this.game.win();
    return Math.floor(alive / (alive + dead) * 100);
  }
  updateBoids(time) {
    this.kingBoid.update(time);
    for (let i = 0; i < this.numBoids; i++) this.boids[i].update(time, this.map, this.boids, this.kingBoid);
    if (this.numBoids === 0) this.game.lose();
  }
  static toDisplayCoords(c, center) {
    return new Pair(WIDTH / 2 + (c.x - center.x) * ZOOM, HEIGHT / 2 + (c.y - center.y) * ZOOM);
  }
}

// ---- Music --------------------------------------------------------------
const music = {
  audio: new Audio(),
  order: [],
  idx: 0,
  playing: false,
  current: "",
  shuffle() {
    this.order = TRACKS.slice();
    for (let i = this.order.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [this.order[i], this.order[j]] = [this.order[j], this.order[i]];
    }
  },
  next() {
    if (!this.playing) return;
    if (this.idx >= this.order.length) { this.shuffle(); this.idx = 0; }
    const t = this.order[this.idx++];
    this.current = t.name;
    this.audio.src = t.file;
    this.audio.play().catch(() => {});
  },
  start() {
    this.playing = true;
    if (!this.order.length) this.shuffle();
    if (this.audio.src) this.audio.play().catch(() => {}); else this.next();
  },
  stop() { this.playing = false; this.audio.pause(); },
  toggle() { this.playing ? this.stop() : this.start(); }
};
music.audio.addEventListener("ended", () => music.next());
music.audio.addEventListener("error", () => music.next());

// ---- Game ---------------------------------------------------------------
const canvas = document.getElementById("game");
const ctx = canvas.getContext("2d");
const overlay = document.getElementById("overlay");
const startBtn = document.getElementById("start");

const game = {
  world: null, mouse: null, lost: false, won: false, endedAt: 0, started: false,
  lose() { if (!this.lost && !this.won) { this.lost = true; this.endedAt = performance.now(); } },
  win() { if (!this.lost && !this.won) { this.won = true; this.endedAt = performance.now(); } },
  reset() {
    this.lost = this.won = false;
    this.world = new World(WORLD_DIM, this);
    if (this.mouse) this.world.kingBoid.follow(this.mouse);
  }
};

function pointerToCanvas(e) {
  const r = canvas.getBoundingClientRect();
  return new Pair((e.clientX - r.left) * WIDTH / r.width, (e.clientY - r.top) * HEIGHT / r.height);
}
function steer(e) {
  game.mouse = pointerToCanvas(e);
  if (game.world) game.world.kingBoid.follow(game.mouse);
}
canvas.addEventListener("pointermove", steer);
canvas.addEventListener("pointerdown", e => {
  if (!game.started) return;
  if ((game.lost || game.won) && performance.now() - game.endedAt > 600) game.reset();
  steer(e);
});
window.addEventListener("keyup", e => { if (e.key === "m" || e.key === "M") music.toggle(); });

function drawPanel() {
  const w = game.world, x = 20, y = 20, border = 9;
  ctx.fillStyle = "rgb(246,198,0)";
  ctx.fillRect(x, y, 135, 200);
  ctx.fillStyle = "rgb(254,249,180)";
  ctx.fillRect(x + border, y + border, 135 - 2 * border, 200 - 2 * border);
  ctx.drawImage(sprites.live2, x + 20, y + 20, 40, 40);
  ctx.drawImage(sprites.dead2, x + 20, y + 80, 40, 40);
  ctx.drawImage(sprites.locust_swarm, x + 20, y + 140, 35, 35);
  const pct = w.getPercentAlive();
  ctx.fillStyle = "#000";
  ctx.font = "12px sans-serif";
  ctx.textAlign = "left";
  ctx.textBaseline = "alphabetic";
  ctx.fillText(pct + "%", x + 80, y + 45);
  ctx.fillText((100 - pct) + "%", x + 80, y + 105);
  ctx.fillText(String(w.numBoids), x + 80, y + 165);
  if (music.playing && music.current) {
    ctx.font = "bold 17px Courier, monospace";
    ctx.fillStyle = "rgb(219,219,219)";
    ctx.fillText("\u{1F3A7} " + music.current, x + 135 + 15, y + 25);
  }
}

function drawMessage(text, color) {
  ctx.font = "bold 175px monospace";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillStyle = color;
  ctx.fillText(text, WIDTH / 2, HEIGHT / 2);
  ctx.font = "bold 28px monospace";
  ctx.fillStyle = "#fff";
  ctx.fillText("click to play again", WIDTH / 2, HEIGHT / 2 + 130);
}

function render() {
  const w = game.world, c = w.kingBoid.position;
  ctx.clearRect(0, 0, WIDTH, HEIGHT);
  for (const row of w.map) for (const t of row) t.draw(ctx, c);
  if (!game.lost && !game.won) {
    for (let i = 0; i < w.numBoids; i++) w.boids[i].draw(ctx, w);
    w.kingBoid.draw(ctx);
  }
  drawPanel();
  if (game.lost) drawMessage("YOU LOST", "red");
  if (game.won) drawMessage("YOU WON", "lime");
}

let last = 0, acc = 0;
function frame(now) {
  requestAnimationFrame(frame);
  if (!game.started) return;
  acc += Math.min((now - last) / 1000, 0.25);
  last = now;
  const step = 1 / FPS;
  while (acc >= step) {
    if (!game.lost && !game.won) game.world.updateBoids(step);
    acc -= step;
  }
  render();
}

loadSprites().then(() => {
  game.reset();
  startBtn.disabled = false;
  startBtn.textContent = "Click to play";
  startBtn.addEventListener("click", () => {
    overlay.classList.add("hidden");
    game.started = true;
    last = performance.now();
    music.start();
  });
}).catch(err => { startBtn.textContent = "Failed to load"; console.error(err); });
requestAnimationFrame(frame);
