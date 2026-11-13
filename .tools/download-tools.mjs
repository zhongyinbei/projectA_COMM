import { createWriteStream, mkdirSync, statSync } from 'node:fs';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import path from 'node:path';

const dir = 'D:/zhongyinbei/.tools';
mkdirSync(dir, { recursive: true });

const files = [
  {
    name: 'jdk17.zip',
    url: 'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip'
  },
  {
    name: 'maven.zip',
    url: 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'
  }
];

for (const file of files) {
  const target = path.join(dir, file.name);
  console.log(`DOWNLOAD ${file.url}`);
  const res = await fetch(file.url, { redirect: 'follow', signal: AbortSignal.timeout(600000) });
  console.log(`STATUS ${res.status} ${res.headers.get('content-length') || ''} final=${res.url}`);
  if (!res.ok || !res.body) throw new Error(`HTTP ${res.status} for ${file.url}`);
  await pipeline(Readable.fromWeb(res.body), createWriteStream(target));
  console.log(`SAVED ${target} ${statSync(target).size}`);
}
console.log('ALL_DOWNLOADS_DONE');
