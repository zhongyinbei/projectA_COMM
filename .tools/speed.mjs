const urls = [
  ['ms', 'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip'],
  ['huawei', 'https://mirrors.huaweicloud.com/openjdk/17.0.2/openjdk-17.0.2_windows-x64_bin.zip'],
  ['maven', 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip']
];
for (const [name, url] of urls) {
  const t0 = Date.now();
  try {
    const res = await fetch(url, { headers: { Range: 'bytes=0-1048575' }, redirect: 'follow', signal: AbortSignal.timeout(60000) });
    const b = Buffer.from(await res.arrayBuffer());
    console.log(`${name} status=${res.status} bytes=${b.length} ms=${Date.now()-t0} kbps=${Math.round(b.length/1024/((Date.now()-t0)/1000))}`);
  } catch(e) { console.log(`ERR ${name} ${e.message} ms=${Date.now()-t0}`); }
}
