async function test(name, url, chunkSize) {
  const ranges = [];
  for (let i=0;i<8;i++) ranges.push([i*chunkSize, (i+1)*chunkSize-1]);
  const t0=Date.now();
  const results = await Promise.all(ranges.map(async ([s,e]) => {
    try {
      const res = await fetch(url,{headers:{Range:`bytes=${s}-${e}`},redirect:'follow',signal:AbortSignal.timeout(120000)});
      const b = await res.arrayBuffer();
      return b.byteLength;
    } catch(e) { return -1; }
  }));
  const total=results.reduce((a,b)=>a+Math.max(0,b),0);
  console.log(`${name} total=${total} ms=${Date.now()-t0} kbps=${Math.round(total/1024/((Date.now()-t0)/1000))} results=${results.join(',')}`);
}
await test('ms','https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip', 2097152);
await test('huawei','https://mirrors.huaweicloud.com/openjdk/17.0.2/openjdk-17.0.2_windows-x64_bin.zip', 2097152);
