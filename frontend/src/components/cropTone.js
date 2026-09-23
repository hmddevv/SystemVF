/*
 * Màu theo loại cây — file ánh xạ DUY NHẤT (plan mục 3). Mỗi loại cây một màu ở mọi nơi.
 * So khớp theo tên cây (Crop.name), bỏ qua phần giống trong ngoặc: "Cà phê (Robusta)" → cà phê.
 *
 * Lưu ý khả năng tiếp cận: bộ màu này KHÔNG đạt kiểm tra mù màu (leaf ↔ harvest gần như trùng với
 * người mù màu đỏ-lục; harvest ↔ pepper gần nhau cả với mắt thường). Vì vậy màu cây luôn đi kèm
 * tên cây — không bao giờ để màu là kênh nhận diện duy nhất.
 */

const TONES = [
  ['cà phê', 'leafdeep'],
  ['hồ tiêu', 'pepper'],
  ['sầu riêng', 'harvest'],
  ['cao su', 'leaf'],
  ['điều', 'clay'],
];

const FALLBACK = 'sky';

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
