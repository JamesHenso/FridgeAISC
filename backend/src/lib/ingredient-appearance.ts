// Category is culinary meaning; icons can be more specific (cut of meat, type of seasoning).
// Match whole words/prefixes, not arbitrary substrings such as "lê" inside "phi lê".
export interface IngredientAppearance { category: string; image_url: string | null }
const icon = (file: string) => `/assets/${encodeURIComponent(file)}.webp`;
export function ingredientAppearance(name: string): IngredientAppearance {
  const n = name.normalize('NFC').trim().toLocaleLowerCase('vi');
  const starts = (pattern: string) => new RegExp(`^(?:${pattern})(?=$|[^\\p{L}\\p{N}])`, 'u').test(n);
  const has = (pattern: string) => new RegExp(`(?:^|[^\\p{L}\\p{N}])(?:${pattern})(?=$|[^\\p{L}\\p{N}])`, 'u').test(n);
  const result = (category: string, file?: string): IngredientAppearance => ({ category, image_url: file ? icon(file) : null });
  if (!n || starts('bữa|ăn|hoặc ăn kèm|tráng miệng|món|thực đơn|trang trí|que xiên|tăm|dây|giấy|túi bắt|khuôn|vỏ nghêu') || n.startsWith('(')) return result('Cần rà soát');
  if (starts('bơ đậu phộng')) return n.includes(',') ? result('Cần rà soát') : result('Đậu & hạt');
  if (starts('sữa|váng sữa|phô mai|phô mát|phomai|cream cheese|whipping cream|creamer|kem|yogurt|yaourt')) return result('Sữa & phô mai');
  if (starts('bơ') && !has('booth|034|trái|quả') && !['bơ','bơ sáp'].includes(n)) return result('Bơ & chất béo', 'bo');
  if (starts('bánh tráng|vỏ nem')) return result('Bánh tráng', 'banh_trang');
  if (starts('bánh mì|bánh mỳ|bánh bao|bánh giò|bánh xếp|bánh mandu|bánh healthy|bánh cuốn|bánh phồng|bánh ú|bánh flan|bánh gạo|pizza|cháo|tokbokki|snack|há cảo|xôi|cơm|canh tổ yến')) return result('Bánh & thực phẩm chế biến');
  if (starts('gvns') && !has('bột')) return result('Gia vị & nước chấm', 'hat_nem');
  if (starts('bột bánh|bôt bánh')) return result('Ngũ cốc & bột');
  if (starts('bột|bôt|gvns|gạo|nếp|thính|yến mạch')) {
    if (has('ngọt|nêm|canh|ngũ vị|cà ri|nghệ|hành|tỏi|đinh hương|điều|phô mai')) return result('Gia vị & nước chấm', 'hat_nem');
    return result('Ngũ cốc & bột');
  }
  if (starts('mì|mỳ|bún|phở|miến|nui|hủ tiếu|bánh canh|bánh phở|bánh đa|bánh hỏi|spaghetti|udon')) return result('Bún, mì & phở', 'bun');
  if (starts('chà bông|chả bông|ruốc thịt')) return result('Thịt chế biến', 'cha_bong_gion');
  if (starts('mắm tôm|mắm ruốc')) return result('Mắm', 'mam_tom');
  if (starts('đường|mật ong|mạch nha')) return result('Đường & chất tạo ngọt', 'duong');
  if (starts('dầu hào|dầu giấm')) return result('Gia vị & nước chấm', 'hat_nem');
  if (starts('cốt lết')) return result('Thịt heo', 'suon');
  if (starts('dầu|mỡ|màu dầu')) return result('Dầu & mỡ');
  if (starts('nước cốt dừa')) return result('Sữa & phô mai');
  if (starts('nước mắm|nước nắm|nước tương|nước chấm|nước màu|nước dùng|nước nấu|nước cốt|nước me|nước sả|nước nghệ|nước hành|nước bột|nước thơm|gia vị|hạt nêm|muối|tiêu|tương|xốt|sốt|mắm|giấm|sa tế|satế|mayonnaise|chao|cốt|cà paste|cà hộp|thuốc muối|ngũ vị|ớt bột|ớt satế|quế|hồi|đinh hương|kỷ tử|kỉ tử|hạt mắc khén|hoa hồi|mù tạt|mù tạc|mayo|ketchup|mẻ|hắc xì dầu')) return result('Gia vị & nước chấm', 'hat_nem');
  if (starts('nước|trà|bia|rượu|cà phê|thức uống|sinh tố')) return result('Đồ uống');
  if (starts('đậu hũ|đậu hủ|đậu phụ|tàu hũ|tàu hủ|tàu ky|lá tàu hủ ky|lá tàu hũ ky|hem chay|sườn chay|sườn non chay|chả giò chay|chả giò rế chay')) return result('Đậu phụ & thực phẩm chay');
  if (starts('chả cá|chả tôm|chả mực')) return result('Cá & hải sản', 'hai_san');
  if (starts('xúc xích|lạp xưởng|chả|giò sống|giò thủ|nem|pate|pa tê|jambon|bacon|thịt nguội|thịt xông|khô bò')) return result('Thịt chế biến');
  if (starts('trứng|lòng đỏ trứng|lòng trắng trứng|hột vịt')) return has('cá|tôm') ? result('Cá & hải sản', 'hai_san') : result('Trứng', 'gia_cam');
  if ((starts('thịt|ức|lườn|bao tử|đầu|râu|khô') && has('ốc|sò|lươn|cá|mực|tôm')) || starts('surimi|thanh cua|bào ngư')) return result('Cá & hải sản', 'hai_san');
  if (starts('cá|tôm|tép|mực|bạch tuộc|ốc|nghêu|sò|cua|ghẹ|hàu|hến|lươn|càng ghẹ|viên hải sản|viên thả lẩu|đậu cá|thịt hến|thịt nghêu|thịt cua')) return result('Cá & hải sản', 'hai_san');
  if (starts('gà|vịt|ngan|ngỗng|chim|ức|cánh|đùi|má đùi|thịt|xương|chân|mề|gan|tim') && has('gà|vịt|ngan|ngỗng|chim')) return result('Thịt gia cầm', 'thit_ga');
  if (starts('bò|bê|thịt|bắp|nạm|thăn|gân|đuôi|sườn|ba chỉ|ba rọi|gan|tim|đùi|xương') && has('bò|bê')) return result('Thịt bò & bê', 'thit_bo');
  if (starts('thịt gấc')) return result('Trái cây', 'trai_cay');
  if (starts('sườn')) return result('Thịt heo', 'suon');
  if (starts('xương')) return result('Thịt heo', 'xuong');
  if (has('ba chỉ|ba rọi')) return result('Thịt heo', 'ba_chi');
  if (starts('chân giò|bắp giò|giò heo|dựng heo')) return result('Thịt heo', 'bap, chan_gio');
  if (starts('thịt đùi|đùi heo')) return result('Thịt heo', 'thit_dui');
  if (starts('nạc|thịt|heo|lợn|tai heo|da heo|gan heo|tim heo|dồi|lòng|bóng bì|thăn')) return has('dê|ếch|thỏ') ? result('Thịt khác') : result('Thịt heo', 'thit-heo');
  if (starts('ếch|dê|thỏ')) return result('Thịt khác');
  if (starts('nấm|mộc nhĩ')) return result('Nấm', 'nam');
  if (starts('khoai|củ|cà rốt|su hào|gừng|riềng|nghệ|hành tây|hành tím|hành khô|tỏi|radish|kiệu')) return result('Củ & gia vị tươi', 'cu');
  if (starts('đậu xanh|đậu đỏ|đậu đen|đậu nành|đậu phộng|lạc|hạt sen|hạt điều|mè|vừng|hạt óc chó|hạnh nhân|hạt é|hạt thông|hạt tàu xì|hạt đác|bạch quả')) return result('Đậu & hạt');
  if (starts('măng cụt')) return result('Trái cây', 'trai_cay');
  if (starts('rau|các loại rau|escarol|gốc hành|boaro|boa rô|tía tô|bồn bồn|cọng|điên điển|đồ chua|phổ tai|cà bi|kinh giới|xá bấu|dưa gừng|cải|bắp cải|bắp chuối|hoa chuối|bông|lá|đọt|ngò|hành|gốc ngò|cọng ngò|đầu hành|măng|bí|dưa leo|dưa chuột|dưa cải|xà lách|hẹ|răm|hương thảo|thì là|rong|đậu|sen|ngó sen|mướp|khổ qua|bầu|sả|húng|mint|basil|oregano|mồng tơi|cần|bạc hà|tần ô|diếp|cà chua|cà pháo|cà tím|ớt|su su|kim chi|bắp|ngô|salad|canh rau|giá|dọc mùng|kèo nèo')) return result('Rau & quả dùng nấu ăn', 'rau');
  if (starts('cóc|khế|quả|vỏ chanh|vỏ tắc|mía|ổi|nho|lê|xoài|táo|chuối|cam|quýt|dưa hấu|thanh long|đu đủ|dưa lưới|cherry|dâu|mận|đào|kiwi|bơ|chanh|lựu|hồng|mít|bưởi|dứa|thơm|tắc|quất|dừa|gấc|trái|vỏ cam|me')) return result('Trái cây', 'trai_cay');
  return result('Khác');
}
