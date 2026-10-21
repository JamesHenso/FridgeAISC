import { it } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { ingredientAppearance } from '../src/lib/ingredient-appearance.js';
it('uses the product identity before flavor, cut, or substring matches', () => {
  for (const [name, category] of [
    ['Chả cá','Cá & hải sản'], ['Cốt lết bỏ xương','Thịt heo'], ['Măng cụt','Trái cây'], ['Dầu hào','Gia vị & nước chấm'],
    ['Lá tàu hủ ky khô','Đậu phụ & thực phẩm chay'], ['Bột bánh rán vị phô mai','Ngũ cốc & bột'], ['GVNS ® Lẩu kim chi','Gia vị & nước chấm'],
    ['Cá lóc phi lê','Cá & hải sản'], ['Bắp bò','Thịt bò & bê'], ['Bắp cải tím','Rau & quả dùng nấu ăn'],
    ['Sữa chua ít đường','Sữa & phô mai'], ['Mì gà phô mai','Bún, mì & phở'], ['Đùi tỏi gà','Thịt gia cầm'],
    ['Bột mì','Ngũ cốc & bột'], ['Khoai mì','Củ & gia vị tươi'], ['Hạt nêm thịt heo','Gia vị & nước chấm'],
    ['Mực trứng','Cá & hải sản'], ['Trứng gà','Trứng'], ['Ba chỉ bò Úc','Thịt bò & bê'],
    ['Mắm tôm','Mắm'], ['Bơ Booth','Trái cây'], ['Bơ thực vật','Bơ & chất béo'],
    ['Bữa trưa: Ăn kèm Cơm, Canh Cá','Cần rà soát'], ['Nấm bào ngư','Nấm'], ['Chà bông giòn','Thịt chế biến'],
  ]) assert.equal(ingredientAppearance(name).category, category, name);
});
it('references real, URL-encoded asset paths and does not force an unrelated icon', () => {
  for (const name of ['Chân giò heo','Thịt ba chỉ','Sườn non','Đùi heo','Xương heo','Thịt heo','Nấm','Rau cải','Cà rốt','Đường','Bún','Bơ lạt','Bánh tráng','Chà bông giòn','Mắm tôm','Cá','Bò','Gà','Trứng','Táo','Hạt nêm']) {
    const path=ingredientAppearance(name).image_url!;
    assert.ok(path.startsWith('/assets/'));
    assert.ok(!path.includes(' '));
    assert.ok(existsSync(fileURLToPath(new URL('../'+decodeURIComponent(path.slice(1)),import.meta.url))),path);
  }
  assert.equal(ingredientAppearance('Sữa tươi').image_url, null);
});
