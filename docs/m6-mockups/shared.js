/*
 * Dữ liệu và hành vi dùng chung cho ba mockup M6.
 * Dữ liệu mô phỏng đúng hình dạng API (ReminderResponse, SeasonResponse, ActivityResponse,
 * HarvestResponse) để khi dựng frontend thật không phải nghĩ lại. "Hôm nay" cố định là
 * 23/09/2026 — giữa mùa mưa, nên luật CARE-02 (bón phân mùa mưa) đang chạy.
 */
(function () {
  'use strict';

  var TODAY = '2026-09-23';

  // Nhãn tiếng Việt cho enum: ở frontend thật đây là file ánh xạ duy nhất.
  var ACTIVITY = {
    WATERING: 'Tưới nước',
    FERTILIZING: 'Bón phân',
    SPRAYING: 'Phun thuốc',
    WEEDING: 'Làm cỏ',
    PRUNING: 'Tỉa cành',
    OTHER: 'Việc khác'
  };

  var ICON = {
    WATERING: '<path d="M12 3c3.5 4.6 6 7.9 6 11a6 6 0 0 1-12 0c0-3.1 2.5-6.4 6-11z"/>',
    FERTILIZING: '<path d="M7 8h10l1 12H6L7 8z"/><path d="M9 8V5h6v3"/><circle cx="10" cy="14" r="1"/><circle cx="14" cy="16" r="1"/><circle cx="12" cy="12" r="1"/>',
    SPRAYING: '<path d="M8 10h7v10H8z"/><path d="M10 10V6h3v4"/><path d="M13 6h3"/><path d="M18 4l2-1M18 6h3M18 8l2 1"/>',
    WEEDING: '<path d="M4 20h16"/><path d="M8 20c0-4-2-6-4-7"/><path d="M12 20V9"/><path d="M12 13c2-3 4-4 7-4"/><path d="M12 11c-2-3-4-4-6-4"/>',
    PRUNING: '<circle cx="6" cy="18" r="2.5"/><circle cx="6" cy="6" r="2.5"/><path d="M8 7.5L20 16M8 16.5L20 8"/>',
    OTHER: '<circle cx="6" cy="12" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="18" cy="12" r="1.5"/>',
    HARVEST: '<circle cx="8" cy="15" r="3.5"/><circle cx="15.5" cy="16" r="3.5"/><path d="M8 11.5C9 8 11 6 14 4M15.5 12.5C15 9 14.5 7 14 4"/>'
  };

  function icon(type, cls) {
    return '<svg class="' + (cls || 'ico') + '" viewBox="0 0 24 24" aria-hidden="true" fill="none" ' +
      'stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' +
      ICON[type] + '</svg>';
  }

  var farm = { name: 'Vườn nhà', location: 'Cư M\'gar, Đắk Lắk' };

  // GET /reminders — đã sắp: quá hạn trước, hạn gần trước.
  var reminders = [
    { id: 'r1', plantingId: 11, plotName: 'Lô A2', cropName: 'Cà phê Robusta', severity: 'OVERDUE',
      daysOverdue: 10, dueDate: '2026-09-13', suggestedActivity: 'FERTILIZING', ruleCode: 'CARE-02',
      title: 'Bón phân mùa mưa', detail: 'Lần bón gần nhất 30/07. Đất đang đủ ẩm, cây hấp thụ tốt.' },
    { id: 'r2', plantingId: 12, plotName: 'Lô A2', cropName: 'Hồ tiêu Vĩnh Linh', severity: 'OVERDUE',
      daysOverdue: 8, dueDate: '2026-09-15', suggestedActivity: 'FERTILIZING', ruleCode: 'CARE-02',
      title: 'Bón phân mùa mưa', detail: 'Lần bón gần nhất 01/08.' },
    { id: 'r3', plantingId: 21, plotName: 'Lô B1', cropName: 'Sầu riêng Ri6', severity: 'DUE_SOON',
      daysOverdue: -3, dueDate: '2026-09-26', suggestedActivity: 'FERTILIZING', ruleCode: 'CARE-02',
      title: 'Bón phân mùa mưa', detail: 'Lần bón gần nhất 12/08.' },
    { id: 'r4', plantingId: 21, plotName: 'Lô B1', cropName: 'Sầu riêng Ri6', severity: 'DUE_SOON',
      daysOverdue: -5, dueDate: '2026-09-28', suggestedActivity: 'OTHER', ruleCode: 'CARE-04',
      title: 'Thăm vườn kiến thiết cơ bản', detail: 'Vườn 2 năm tuổi, chưa cho thu hoạch. Lần thăm gần nhất 29/08.' }
  ];

  // GET /plantings/11/seasons + /seasons/{id}/activities + /harvests, đã trộn theo ngày.
  var seasons = {
    '2026': {
      label: '2026/2027', startDate: '2026-02-01', endDate: '2027-01-31', current: true,
      entries: [
        { date: '2026-07-30', kind: 'activity', type: 'FERTILIZING', note: 'NPK 16-16-8, 500 kg, đợt 2', cost: 12400000 },
        { date: '2026-07-02', kind: 'activity', type: 'SPRAYING', note: 'Trừ rệp sáp', cost: 3150000 },
        { date: '2026-06-18', kind: 'activity', type: 'WEEDING', note: 'Thuê 4 công', cost: 2600000 },
        { date: '2026-05-25', kind: 'activity', type: 'FERTILIZING', note: 'NPK 16-16-8, 480 kg, đợt 1', cost: 11800000 },
        { date: '2026-04-10', kind: 'activity', type: 'WATERING', note: 'Đợt 3', cost: 1900000 },
        { date: '2026-03-18', kind: 'activity', type: 'WATERING', note: 'Đợt 2', cost: 1900000 },
        { date: '2026-02-25', kind: 'activity', type: 'WATERING', note: 'Tưới hoa', cost: 2100000 },
        { date: '2026-02-12', kind: 'activity', type: 'PRUNING', note: 'Tỉa cành sau thu hoạch', cost: 1500000 }
      ]
    },
    '2025': {
      label: '2025/2026', startDate: '2025-02-01', endDate: '2026-01-31', current: false,
      entries: [
        { date: '2026-01-05', kind: 'harvest', qty: 500, revenue: 52000000 },
        { date: '2025-12-08', kind: 'harvest', qty: 1400, revenue: 141400000 },
        { date: '2025-11-20', kind: 'harvest', qty: 1200, revenue: 118800000 },
        { date: '2025-11-02', kind: 'activity', type: 'OTHER', note: 'Thuê bạt phơi, sân phơi', cost: 4500000 },
        { date: '2025-08-04', kind: 'activity', type: 'FERTILIZING', note: 'NPK đợt 2', cost: 11600000 },
        { date: '2025-07-10', kind: 'activity', type: 'SPRAYING', note: 'Trừ rệp sáp', cost: 2900000 },
        { date: '2025-06-02', kind: 'activity', type: 'FERTILIZING', note: 'NPK đợt 1', cost: 11200000 },
        { date: '2025-04-12', kind: 'activity', type: 'WATERING', note: 'Đợt 3', cost: 1800000 },
        { date: '2025-03-15', kind: 'activity', type: 'WATERING', note: 'Đợt 2', cost: 1800000 },
        { date: '2025-02-22', kind: 'activity', type: 'WATERING', note: 'Tưới hoa', cost: 2000000 }
      ]
    }
  };

  var money = new Intl.NumberFormat('vi-VN');

  function fmtMoney(n) { return money.format(n) + ' đ'; }

  function fmtDay(iso) {
    var p = iso.split('-');
    return p[2] + '/' + p[1];
  }

  function fmtTodayLong() { return 'Thứ Tư, 23 tháng 9'; }

  function totals(season) {
    var cost = 0, revenue = 0, qty = 0;
    season.entries.forEach(function (e) {
      if (e.kind === 'activity') cost += e.cost; else { revenue += e.revenue; qty += e.qty; }
    });
    return { cost: cost, revenue: revenue, qty: qty, net: revenue - cost };
  }

  function dayIndex(iso) { return Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) / 864e5; }

  /*
   * Dải niên vụ: 12 tháng chạy ngang từ tháng bắt đầu niên vụ (lấy từ API, không tự tính).
   * Chi phí là vạch dưới đường gốc, thu hoạch là vạch trên, cùng một thang tiền.
   * Mùa mưa tháng 5–9 là hằng số hiển thị, khớp CARE-02.
   */
  function seasonStrip(season, opt) {
    opt = opt || {};
    var W = 360, H = opt.height || 132, base = opt.base || 66, maxBar = opt.maxBar || 52;
    var start = dayIndex(season.startDate), len = dayIndex(season.endDate) - start + 1;
    var startMonth = +season.startDate.slice(5, 7);
    var colW = W / 12;
    var max = 0;
    season.entries.forEach(function (e) { max = Math.max(max, e.kind === 'activity' ? e.cost : e.revenue); });
    var out = [];
    out.push('<svg class="strip" viewBox="0 0 ' + W + ' ' + H + '" role="img" aria-label="Dải niên vụ ' +
      season.label + ': chi phí dưới đường gốc, thu hoạch trên đường gốc">');
    for (var i = 0; i < 12; i++) {
      var m = ((startMonth - 1 + i) % 12) + 1;
      if (m >= 5 && m <= 9) out.push('<rect class="s-rain" x="' + (i * colW) + '" y="0" width="' + colW + '" height="' + (H - 18) + '"/>');
    }
    for (var j = 1; j < 12; j++) out.push('<line class="s-grid" x1="' + (j * colW) + '" x2="' + (j * colW) + '" y1="4" y2="' + (H - 20) + '"/>');
    out.push('<line class="s-base" x1="0" x2="' + W + '" y1="' + base + '" y2="' + base + '"/>');
    season.entries.forEach(function (e) {
      var x = ((dayIndex(e.date) - start) / len) * W;
      var v = e.kind === 'activity' ? e.cost : e.revenue;
      var h = Math.max(3, (v / max) * maxBar);
      var bw = opt.barWidth || 5;
      if (e.kind === 'activity') {
        out.push('<rect class="s-cost" x="' + (x - bw / 2) + '" y="' + (base + 1) + '" width="' + bw + '" height="' + h + '"/>');
      } else {
        out.push('<rect class="s-harvest" x="' + (x - bw / 2) + '" y="' + (base - h) + '" width="' + bw + '" height="' + h + '"/>');
      }
    });
    var t = dayIndex(TODAY);
    if (t >= start && t < start + len) {
      var tx = ((t - start) / len) * W;
      out.push('<line class="s-today-line" x1="' + tx + '" x2="' + tx + '" y1="2" y2="' + (H - 20) + '"/>');
      out.push('<text class="s-today" x="' + Math.min(tx, W - 30) + '" y="12" text-anchor="middle">hôm nay</text>');
    }
    for (var k = 0; k < 12; k++) {
      var mm = ((startMonth - 1 + k) % 12) + 1;
      out.push('<text class="s-month" x="' + (k * colW + colW / 2) + '" y="' + (H - 4) + '" text-anchor="middle">T' + mm + '</text>');
    }
    out.push('</svg>');
    return out.join('');
  }

  function toast(el, text) {
    if (!el) return;
    el.textContent = text;
    el.classList.add('is-on');
    clearTimeout(el._t);
    el._t = setTimeout(function () { el.classList.remove('is-on'); }, 2600);
  }

  /*
   * Hành vi của form ghi nhanh — giống nhau ở cả ba hướng, chỉ khác giao diện:
   * chọn loại việc bằng nút lớn, ô tiền tự chấm nghìn, "Việc khác" bắt buộc ghi chú (BR-12),
   * ghi xong thì lời nhắc tương ứng gạch ngang rồi rời khỏi danh sách (BR-18).
   */
  function bindLogForm(root) {
    var form = root.querySelector('[data-log-form]');
    if (!form) return;
    var typeBtns = form.querySelectorAll('[data-type]');
    var chosen = form.getAttribute('data-default-type') || null;
    var noteLabel = form.querySelector('[data-note-label]');
    var noteInput = form.querySelector('[data-note]');
    var errType = form.querySelector('[data-err-type]');
    var errNote = form.querySelector('[data-err-note]');

    function sync() {
      typeBtns.forEach(function (b) { b.setAttribute('aria-pressed', String(b.getAttribute('data-type') === chosen)); });
      if (noteLabel) noteLabel.setAttribute('data-required', String(chosen === 'OTHER'));
    }
    typeBtns.forEach(function (b) {
      b.addEventListener('click', function () {
        chosen = b.getAttribute('data-type');
        if (errType) errType.hidden = true;
        sync();
      });
    });
    sync();

    var cost = form.querySelector('[data-money]');
    if (cost) {
      cost.addEventListener('input', function () {
        var digits = cost.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '').slice(0, 13);
        cost.value = digits ? money.format(+digits) : '';
      });
    }

    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var ok = true;
      if (!chosen) { if (errType) errType.hidden = false; ok = false; }
      if (chosen === 'OTHER' && noteInput && !noteInput.value.trim()) {
        if (errNote) errNote.hidden = false;
        noteInput.focus();
        ok = false;
      } else if (errNote) { errNote.hidden = true; }
      if (!ok) return;
      var target = document.querySelector('[data-reminder="' + form.getAttribute('data-target-reminder') + '"]');
      if (target && !target.classList.contains('is-done')) {
        target.classList.add('is-done');
        setTimeout(function () { target.classList.add('is-gone'); }, 900);
      }
      toast(document.querySelector('[data-toast]'), 'Đã ghi');
    });
  }

  function bindSeasonTabs(root, render) {
    var tabs = root.querySelectorAll('[data-season-tab]');
    tabs.forEach(function (t) {
      t.addEventListener('click', function () {
        tabs.forEach(function (x) { x.setAttribute('aria-selected', String(x === t)); });
        render(t.getAttribute('data-season-tab'));
      });
    });
    render('2026');
  }

  function bindReset(root) {
    var btn = root.querySelector('[data-reset]');
    if (btn) btn.addEventListener('click', function () {
      document.querySelectorAll('[data-reminder]').forEach(function (r) { r.classList.remove('is-done', 'is-gone'); });
    });
  }

  window.Farm = {
    TODAY: TODAY, ACTIVITY: ACTIVITY, icon: icon, farm: farm, reminders: reminders, seasons: seasons,
    fmtMoney: fmtMoney, fmtDay: fmtDay, fmtTodayLong: fmtTodayLong, totals: totals,
    seasonStrip: seasonStrip, bindLogForm: bindLogForm, bindSeasonTabs: bindSeasonTabs, bindReset: bindReset
  };
})();
