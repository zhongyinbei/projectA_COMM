const base='http://localhost:8080';
const login=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'admin',password:'admin123'})}).then(r=>r.json());
if(login.code!==0) throw new Error('login failed');
const headers={Authorization:'Bearer '+login.data.token};
const summary=await fetch(base+'/api/dashboard/summary',{headers}).then(r=>r.json());
const rate=await fetch(base+'/api/dashboard/inspection-rate',{headers}).then(r=>r.json());
const orders=await fetch(base+'/api/repair-orders?page=1&size=10',{headers}).then(r=>r.json());
console.log(JSON.stringify({admin:login.data.user,summary:summary.data,rateDays:rate.data.length,orders:orders.data.total},null,2));
