/*
 * Màu theo loại cây — file ánh xạ DUY NHẤT (plan mục 3). Mỗi loại cây một màu ở mọi nơi.
 * So khớp theo tên cây (Crop.name), bỏ qua phần giống trong ngoặc: "Cà phê (Robusta)" → cà phê.
 * Giá trị màu và kết quả kiểm tra mù màu nằm ở token crop-* trong index.css.
 *
 * Bộ màu đã qua kiểm tra mù màu, nhưng màu vẫn không bao giờ là kênh nhận diện duy nhất: chấm
 * màu luôn đi kèm tên cây.
 */

const TONES = [
  ['cà phê', 'crop-coffee'],
  ['hồ tiêu', 'crop-pepper'],
  ['sầu riêng', 'crop-durian'],
  ['cao su', 'crop-rubber'],
  ['điều', 'crop-cashew'],
];

// Cây ngoài danh sách (vd. ngô): xám, không lấy thêm sắc độ — sắc độ thứ sáu sẽ phá độ tách màu.
const FALLBACK = 'crop-other';

export function cropTone(cropName) {
  const name = (cropName ?? '').normalize('NFC').toLowerCase().trim();
  const hit = TONES.find(([prefix]) => name.startsWith(prefix));
  return hit ? hit[1] : FALLBACK;
}

export function cropColor(cropName) {
  return `var(--color-${cropTone(cropName)})`;
}

// "Cà phê (Robusta)" → "Cà phê": gộp các giống cùng loại cây khi cần một nhãn ngắn.
export function cropKind(cropName) {
  return (cropName ?? '').replace(/\s*\(.*\)\s*$/, '');
}
