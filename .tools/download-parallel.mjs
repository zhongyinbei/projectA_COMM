import { createWriteStream, mkdirSync, statSync, existsSync, rmSync } from 'node:fs';
import { writeFile } from 'node:fs/promises';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import path from 'node:path';

const dir = 'D:/zhongyinbei/.tools';
const chunkDir = path.join(dir, 'chunks');
mkdirSync(chunkDir, { recursive: true });

const files = [
  {
    name: 'jdk17.zip',
    url: 'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip',
    size: 186360891
  },
  {
    name: 'maven.zip',
    url: 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip',
    size: 9490398
  }
];

const CHUNK = 4 * 1024 * 1024;
const WORKERS = 8;

async function fetchChunk(url, start, end, attempt = 1) {
  try {
    const res = await fetch(url, {
      headers: { Range: `bytes=${start}-${end}` },
      redirect: 'follow',
      signal: AbortSignal.timeout(180000)
    });
    if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`);
    const buf = Buffer.from(await res.arrayBuffer());
    if (buf.length !== end - start + 1) throw new Error(`size ${buf.length} != ${end - start + 1}`);
    return buf;
  } catch (e) {
    if (attempt >= 3) throw e;
    console.log(`RETRY ${start}-${end} attempt=${attempt + 1} ${e.message}`);
    return fetchChunk(url, start, end, attempt + 1);
  }
}

for (const file of files) {
  const target = path.join(dir, file.name);
  const fileChunkDir = path.join(chunkDir, file.name.replace('.zip', ''));
  mkdirSync(fileChunkDir, { recursive: true });
  const count = Math.ceil(file.size / CHUNK);
  console.log(`START ${file.name} size=${file.size} chunks=${count}`);

  let next = 0;
  let completed = 0;
  async function worker() {
    while (true) {
      const index = next++;
      if (index >= count) return;
      const start = index * CHUNK;
      const end = Math.min(start + CHUNK, file.size) - 1;
      const part = path.join(fileChunkDir, `${String(index).padStart(4, '0')}.part`);
      if (existsSync(part) && statSync(part).size === end - start + 1) {
        completed++;
        continue;
      }
      const buf = await fetchChunk(file.url, start, end);
      await writeFile(part, buf);
      completed++;
      if (completed % 5 === 0 || completed === count) {
        console.log(`PROGRESS ${file.name} ${completed}/${count}`);
      }
    }
  }
  await Promise.all(Array.from({ length: WORKERS }, () => worker()));

  rmSync(target, { force: true });
  const { appendFile } = await import('node:fs/promises');
  for (let i = 0; i < count; i++) {
    const part = path.join(fileChunkDir, `${String(i).padStart(4, '0')}.part`);
    const { readFile } = await import('node:fs/promises');
    await appendFile(target, await readFile(part));
  }
  const actual = statSync(target).size;
  console.log(`SAVED ${target} ${actual}`);
  if (actual !== file.size) throw new Error(`final size mismatch for ${file.name}`);
  rmSync(fileChunkDir, { recursive: true, force: true });
}
console.log('ALL_DOWNLOADS_DONE');
