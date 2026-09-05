import { writeFileSync } from 'node:fs'

const q = v => v === null ? 'NULL' : typeof v === 'number' ? String(v) : `'${String(v).replaceAll("'", "''")}'`
const out = ['-- 开发测试数据，先执行 Flyway；会员密码均为 password。', 'USE mall_system;']
function insert(table, columns, rows) {
  const values = rows.map(row => `(${row.map(q).join(',')})`).join(',\n')
  out.push(`INSERT IGNORE INTO ${table} (${columns.join(',')}) VALUES\n${values};`)
}
const now = '2026-09-04 00:00:00'
insert('pm_category', ['id','category_name','parent_id','level','order_num','status','create_time','update_time'], Array.from({length:10},(_,i)=>[900001+i,`测试类目-${String(i+1).padStart(2,'0')}`,0,1,i+1,1,now,now]))
insert('pm_brand', ['id','brand_name','brand_desc','order_num','create_time','update_time'], Array.from({length:8},(_,i)=>[910001+i,`测试品牌-${String(i+1).padStart(2,'0')}`,'开发环境模拟品牌',i+1,now,now]))
insert('pm_spu', ['id','spu_code','spu_name','category_id','brand_id','description','customs_code','origin_country','status','sales_count','create_time','update_time'], Array.from({length:200},(_,i)=>[920001+i,`DEVSPU${String(i+1).padStart(4,'0')}`,`跨境测试商品${String(i+1).padStart(4,'0')}`,900001+i%10,910001+i%8,JSON.stringify({zh:'开发测试商品',en:'Development test product'}),`HS${String(i+1).padStart(8,'0')}`,['CN','JP','US','DE','KR'][i%5],1,(i+1)*3,now,now]))
insert('pm_sku', ['id','spu_id','sku_code','attrs','price','currency','cost_price','weight','status','create_time','update_time'], Array.from({length:400},(_,i)=>[930001+i,920001+i%200,`DEVSKU${String(i+1).padStart(4,'0')}`,JSON.stringify([{k:'颜色',v:['黑','白','蓝','红','绿'][i%5]}]),(9.9+(i%80)*1.25).toFixed(2),'USD',(5+(i%40)*.55).toFixed(2),(0.1+(i%20)*.05).toFixed(3),1,now,now]))
insert('pm_sku_stock', ['id','sku_id','warehouse_id','stock','locked_stock','create_time','update_time'], Array.from({length:400},(_,i)=>[950001+i,930001+i,1,500,0,now,now]))
insert('mm_member', ['id','email','phone','nick_name','level','points','status','create_time','update_time'], Array.from({length:100},(_,i)=>[940001+i,`dev${String(i+1).padStart(3,'0')}@example.test`,`1390000${String(i+1).padStart(4,'0')}`,`测试会员-${String(i+1).padStart(3,'0')}`,i%3,(i+1)*10,1,now,now]))
insert('member_account', ['member_id','phone','password_hash','status','create_time','update_time'], Array.from({length:100},(_,i)=>[940001+i,`1390000${String(i+1).padStart(4,'0')}`,'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',1,now,now]))
insert('member_address', ['id','user_id','receiver_name','receiver_phone','province','city','district','detail_address','is_default','create_time','update_time'], Array.from({length:100},(_,i)=>[960001+i,940001+i,`测试会员-${String(i+1).padStart(3,'0')}`,`1390000${String(i+1).padStart(4,'0')}`,'测试省','测试市','测试区',`开发路-${i+1}号`,1,now,now]))
insert('trade_cart', ['id','user_id','sku_id','quantity','checked','create_time','update_time'], Array.from({length:100},(_,i)=>[970001+i,940001+i,930001+i%400,i%3+1,1,now,now]))
insert('member_favorite', ['id','user_id','spu_id','create_time','update_time'], Array.from({length:100},(_,i)=>[980001+i,940001+i,920001+i%200,now,now]))
insert('member_browse_history', ['id','user_id','spu_id','create_time','update_time'], Array.from({length:100},(_,i)=>[990001+i,940001+i,920001+(i+20)%200,now,now]))
writeFileSync(new URL('./dev_seed.sql', import.meta.url), `${out.join('\n\n')}\n`)
console.log(`generated ${out.length} statements into scripts/dev_seed.sql`)
