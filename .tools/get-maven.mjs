import { writeFile, mkdir, rm } from 'node:fs/promises';
const url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip';
const size=9202456;
const dir='D:/zhongyinbei/.tools/mvnparts';
await rm(dir,{recursive:true,force:true}); await mkdir(dir,{recursive:true});
const chunk=1048576; const count=Math.ceil(size/chunk);
const parts=[];
for(let i=0;i<count;i++){
  const start=i*chunk, end=Math.min(start+chunk,size)-1;
  parts.push((async()=>{
    for(let a=1;a<=4;a++){
      try{
        const res=await fetch(url,{headers:{Range:`bytes=${start}-${end}`},redirect:'follow',signal:AbortSignal.timeout(120000)});
        const b=Buffer.from(await res.arrayBuffer());
        if(b.length!==end-start+1) throw new Error('size '+b.length);
        await writeFile(`${dir}/${i}.part`,b);
        console.log('OK',i,b.length); return b;
      }catch(e){console.log('RETRY',i,a,e.message); if(a===4) throw e;}
    }
  })());
}
const bufs=await Promise.all(parts);
const { appendFile } = await import('node:fs/promises');
await rm('D:/zhongyinbei/.tools/maven.zip',{force:true});
for(const b of bufs) await appendFile('D:/zhongyinbei/.tools/maven.zip',b);
console.log('DONE');

