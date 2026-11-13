import { writeFile } from 'node:fs/promises';
const url = 'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip';
for (const range of ['bytes=0-1048575','bytes=1048576-2097151']) {
  const t0 = Date.now();
  try {
    const res = await fetch(url, { headers: { Range: range }, redirect: 'follow', signal: AbortSignal.timeout(60000) });
    const b = Buffer.from(await res.arrayBuffer());
    console.log(`${range} status=${res.status} bytes=${b.length} ms=${Date.now()-t0} first=${b[0]}`);
    await writeFile(`D:/zhongyinbei/.tools/chunk-${range.split('=')[1].split('-')[0]}.bin`, b);
  } catch(e) { console.log(`ERR ${range} ${e.message} ms=${Date.now()-t0}`); }
}
