// Preview by default: pnpm tsx scripts/gen-ingredient-category.mjs --dry
// Apply the reviewed rules: pnpm tsx scripts/gen-ingredient-category.mjs --apply
import 'dotenv/config';
import { createClient } from '@supabase/supabase-js';
import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { ingredientAppearance } from '../src/lib/ingredient-appearance.ts';

const apply = process.argv.includes('--apply');
if (apply && process.argv.includes('--dry')) throw new Error('Choose --dry or --apply.');
const sb = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);
const rows = [];
for (let from = 0; ; from += 1000) {
  const { data, error } = await sb.from('Ingredients').select('id,name,category,image_url').order('id').range(from, from + 999);
  if (error) throw new Error(error.message);
  rows.push(...data);
  if (data.length < 1000) break;
}
const plan = rows.map(row => {
  const next = ingredientAppearance(row.name);
  // Preserve custom thumbnails; this script only owns pointers to backend/assets.
  if (row.image_url && !row.image_url.startsWith('/assets/')) next.image_url = row.image_url;
  return { id: row.id, name: row.name, before: { category: row.category, image_url: row.image_url }, after: next };
});
const changes = plan.filter(p => p.before.category !== p.after.category || p.before.image_url !== p.after.image_url);
const reportDir = fileURLToPath(new URL('../reports/', import.meta.url));
mkdirSync(reportDir, { recursive: true });
writeFileSync(reportDir+'ingredient-appearance-plan.json', JSON.stringify(plan, null, 2));
const distribution = plan.reduce((counts,p) => (counts[p.after.category]=(counts[p.after.category]??0)+1,counts),{});
console.log(JSON.stringify({total:rows.length,changes:changes.length,withIcons:plan.filter(p=>p.after.image_url).length,distribution},null,2));
if (!apply) { console.log('DRY RUN — no writes. Review backend/reports/ingredient-appearance-plan.json.'); process.exit(0); }
const backupDir = fileURLToPath(new URL('../backups/', import.meta.url));
mkdirSync(backupDir, { recursive: true });
const backup = backupDir+'ingredients-'+new Date().toISOString().replaceAll(':','-')+'.json';
writeFileSync(backup, JSON.stringify(rows,null,2), { flag:'wx', mode:0o600 });
console.log('Backup saved:',backup);
const groups = new Map();
for(const p of changes){const key=JSON.stringify([p.before,p.after]);if(!groups.has(key))groups.set(key,[]);groups.get(key).push(p);}
let updated=0;
for(const group of groups.values()) {
  const {before,after}=group[0];
  for(let start=0;start<group.length;start+=200){
    const ids=group.slice(start,start+200).map(p=>p.id);
    let q=sb.from('Ingredients').update(after).in('id',ids);
    for(const key of ['category','image_url']) q=before[key]===null?q.is(key,null):q.eq(key,before[key]);
    const {data,error}=await q.select('id');
    if(error)throw new Error(error.message);
    if(data.length!==ids.length)throw new Error('Catalog changed concurrently. Re-run preview before continuing.');
    updated+=data.length;
  }
}
console.log(`Updated ${updated} rows. Re-run --dry to verify zero pending changes.`);
