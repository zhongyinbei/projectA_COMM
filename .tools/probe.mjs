const urls = [
  'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/',
  'https://mirrors.aliyun.com/adoptium/17/jdk/x64/windows/',
  'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip',
  'https://mirrors.huaweicloud.com/openjdk/17.0.2/openjdk-17.0.2_windows-x64_bin.zip',
  'https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip',
  'https://mirrors.aliyun.com/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip',
  'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'
];
for (const url of urls) {
  try {
    const res = await fetch(url, { method: 'GET', headers: { Range: 'bytes=0-1023' }, redirect: 'follow', signal: AbortSignal.timeout(20000) });
    const buf = await res.arrayBuffer();
    console.log(`${res.status} len=${buf.byteLength} final=${res.url} :: ${url}`);
  } catch (e) {
    console.log(`ERR ${e.message} :: ${url}`);
  }
}
