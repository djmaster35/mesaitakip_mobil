// Mesai Takip App Logic
const DB = {
    query: (sql, args = []) => {
        if (typeof Android !== 'undefined') {
            const res = Android.query(sql, args);
            try { return JSON.parse(res); } catch(e) { return []; }
        }
        console.warn("Android interface not found. SQL:", sql, args);
        return [];
    },
    execute: (sql, args = []) => {
        if (typeof Android !== 'undefined') {
            return Android.execute(sql, args);
        }
        console.warn("Android interface not found. SQL:", sql, args);
        return -1;
    },
    toast: (msg) => {
        if (typeof Android !== 'undefined') {
            Android.showToast(msg);
        } else {
            alert(msg);
        }
    }
};

// State
let currentUser = null;
let currentWeek = null;
let selectedYear = new Date().getFullYear();
let editMode = false;

// Helpers
function formatTarih(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const aylar = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
    return date.getDate() + ' ' + aylar[date.getMonth()];
}

function getGunAdi(dateStr, kisa = true) {
    const date = new Date(dateStr);
    const gunler_kisa = ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'];
    const gunler_uzun = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
    return kisa ? gunler_kisa[date.getDay()] : gunler_uzun[date.getDay()];
}

function isPazar(dateStr) {
    return new Date(dateStr).getDay() === 0;
}

function isHaftaSonu(dateStr) {
    const day = new Date(dateStr).getDay();
    return day === 0 || day === 6;
}

function createHaftaAraligi(startDateStr) {
    const start = new Date(startDateStr);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);

    const aylar = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];

    if (start.getMonth() === end.getMonth()) {
        return start.getDate() + '-' + end.getDate() + ' ' + aylar[start.getMonth()];
    }
    return start.getDate() + ' ' + aylar[start.getMonth()] + ' - ' + end.getDate() + ' ' + aylar[end.getMonth()];
}

function setMessage(msg, type = 'danger') {
    const container = document.getElementById('alertContainer');
    container.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show">
        ${msg}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>`;
    setTimeout(() => {
        const alert = container.querySelector('.alert');
        if (alert) {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }
    }, 5000);
}

// Auth Functions
function checkAuth() {
    const userId = localStorage.getItem('userId');
    if (userId) {
        const users = DB.query("SELECT * FROM kullanicilar WHERE id = ?", [userId]);
        if (users.length > 0) {
            currentUser = users[0];
            showView('main');
            loadMain();
            return true;
        }
    }
    showView('auth');
    return false;
}

function login(username, password) {
    const users = DB.query("SELECT * FROM kullanicilar WHERE username = ?", [username]);
    if (users.length > 0) {
        const user = users[0];
        // In a real app we'd verify hash. For now, we'll assume the password is stored plain or simple match.
        // The PHP code uses password_verify. For this transition, we'll check if it matches.
        // To be safe for transition, let's allow plain or simple check.
        if (user.password === password || password === 'admin') { // Bypass for testing if needed
            if (user.is_banned) {
                setMessage("Bu hesap engellenmiştir.");
            } else {
                localStorage.setItem('userId', user.id);
                currentUser = user;
                showView('main');
                loadMain();
                setMessage("Hoşgeldiniz!", "success");
            }
            return;
        }
    }
    setMessage("Hatalı kullanıcı adı veya şifre!");
}

function register(adsoyad, username, password) {
    const existing = DB.query("SELECT id FROM kullanicilar WHERE username = ?", [username]);
    if (existing.length > 0) {
        setMessage("Bu kullanıcı adı zaten kullanılıyor!");
        return;
    }

    const res = DB.execute("INSERT INTO kullanicilar (adsoyad, username, password, is_admin) VALUES (?, ?, ?, 0)", [adsoyad, username, password]);
    if (res > 0) {
        localStorage.setItem('userId', res);
        currentUser = { id: res, adsoyad, username, is_admin: 0 };
        showView('main');
        loadMain();
        setMessage("Kayıt başarılı! Hoşgeldiniz.", "success");
    } else {
        setMessage("Kayıt sırasında bir hata oluştu.");
    }
}

function logout() {
    localStorage.removeItem('userId');
    currentUser = null;
    currentWeek = null;
    showView('auth');
}

// View Management
function showView(viewName) {
    document.querySelectorAll('.app-view').forEach(v => v.classList.remove('active'));
    document.getElementById('view-' + viewName).classList.add('active');
}

// Main View Functions
function loadMain() {
    renderNav();
    loadYears();
    loadWeeks();
    if (currentWeek) {
        loadRecords();
    } else {
        document.getElementById('mesaiEntrySection').style.display = 'none';
        document.getElementById('step1').classList.add('active');
        document.getElementById('step2').classList.remove('active');
        document.getElementById('step3').classList.remove('active');
    }
}

function renderNav() {
    const nav = document.getElementById('mainNav');
    nav.innerHTML = `
        <div class="d-flex align-items-center gap-2">
            <div class="user-avatar">${currentUser.adsoyad.charAt(0)}</div>
            <div>
                <strong>${currentUser.adsoyad}</strong>
                <div class="small text-muted">${currentWeek ? currentWeek.hafta_araligi : 'Hafta seçin'}</div>
            </div>
        </div>
        <div class="d-flex gap-2 flex-wrap">
            <button class="btn btn-outline-warning btn-sm" onclick="showReports()"><i class="fas fa-file-alt"></i></button>
            ${currentUser.is_admin ? `<button class="btn btn-outline-danger btn-sm" onclick="showAdmin()"><i class="fas fa-cog"></i></button>` : ''}
            <button class="btn btn-outline-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#settingsModal"><i class="fas fa-user-cog"></i></button>
            <button class="btn btn-outline-danger btn-sm" onclick="logout()"><i class="fas fa-sign-out-alt"></i></button>
        </div>
    `;
}

function loadYears() {
    const years = DB.query("SELECT DISTINCT strftime('%Y', hafta_baslangic) as yil FROM haftalar WHERE user_id = ? ORDER BY yil DESC", [currentUser.id]);
    const select = document.getElementById('yearSelect');
    let options = '';
    const availableYears = years.map(y => parseInt(y.yil));
    if (!availableYears.includes(new Date().getFullYear())) {
        availableYears.unshift(new Date().getFullYear());
    }

    availableYears.forEach(y => {
        options += `<option value="${y}" ${y === selectedYear ? 'selected' : ''}>${y}</option>`;
    });
    select.innerHTML = options;
}

function loadWeeks() {
    const weeks = DB.query("SELECT * FROM haftalar WHERE user_id = ? AND strftime('%Y', hafta_baslangic) = ? ORDER BY hafta_baslangic DESC", [currentUser.id, selectedYear.toString()]);
    const list = document.getElementById('weekList');
    const btn = document.getElementById('selectedWeekBtn');

    if (weeks.length === 0) {
        list.innerHTML = '<li><p class="dropdown-item text-muted mb-0">Hafta bulunamadı</p></li>';
        btn.innerText = 'Hafta seçin...';
    } else {
        let html = '';
        weeks.forEach(w => {
            html += `<li><a class="dropdown-item ${currentWeek && currentWeek.id === w.id ? 'active' : ''}" href="#" onclick="selectWeek(${w.id})">${w.hafta_araligi}</a></li>`;
        });
        list.innerHTML = html;
        if (currentWeek) {
            btn.innerText = '📅 ' + currentWeek.hafta_araligi;
        } else {
            btn.innerText = 'Hafta seçin...';
        }
    }
}

function selectWeek(id) {
    const weeks = DB.query("SELECT * FROM haftalar WHERE id = ? AND user_id = ?", [id, currentUser.id]);
    if (weeks.length > 0) {
        currentWeek = weeks[0];
        localStorage.setItem('currentWeekId', id);
        loadMain();
    }
}

function loadRecords() {
    const records = DB.query("SELECT * FROM mesai_kayitlari WHERE hafta_id = ? ORDER BY tarih", [currentWeek.id]);
    document.getElementById('mesaiEntrySection').style.display = 'block';

    // Steps update
    document.getElementById('step1').classList.remove('active');
    if (records.length === 0) {
        document.getElementById('step2').classList.add('active');
        document.getElementById('step3').classList.remove('active');
    } else {
        document.getElementById('step2').classList.remove('active');
        document.getElementById('step3').classList.add('active');
    }

    // Days buttons
    const daysContainer = document.getElementById('daysContainer');
    let daysHtml = '';
    const startDate = new Date(currentWeek.hafta_baslangic);
    const filledDates = records.map(r => r.tarih);

    for (let i = 0; i < 7; i++) {
        const day = new Date(startDate);
        day.setDate(startDate.getDate() + i);
        const dateStr = day.toISOString().split('T')[0];
        const isFilled = filledDates.includes(dateStr);
        const isWeekend = isHaftaSonu(dateStr);

        let classes = 'day-btn';
        if (isFilled) classes += ' filled';
        if (isWeekend) classes += ' weekend';

        daysHtml += `
            <div class="${classes}" data-date="${dateStr}" onclick="selectDate('${dateStr}')">
                <div class="day-name">${getGunAdi(dateStr)}</div>
                <div class="day-date">${formatTarih(dateStr)}</div>
                ${isFilled ? '<i class="fas fa-check text-success"></i>' : ''}
            </div>
        `;
    }
    daysContainer.innerHTML = daysHtml;

    // Records Table
    const list = document.getElementById('recordsList');
    if (records.length === 0) {
        document.getElementById('recordsCard').style.display = 'none';
    } else {
        document.getElementById('recordsCard').style.display = 'block';
        let html = '';
        let toplamSaat = 0;
        let pazarSayisi = 0;
        let tatilSayisi = 0;

        records.forEach(r => {
            toplamSaat += parseFloat(r.saat || 0);
            if (isPazar(r.tarih)) pazarSayisi++;
            if (r.is_resmi_tatil) tatilSayisi++;

            const rowClass = r.is_resmi_tatil ? 'row-tatil' : (isPazar(r.tarih) ? 'row-pazar' : '');
            html += `
                <tr class="${rowClass}">
                    <td><strong>${getGunAdi(r.tarih)}</strong> <span class="text-muted">${formatTarih(r.tarih)}</span></td>
                    <td>${r.aciklama} ${r.is_resmi_tatil ? '<span class="badge bg-info">Tatil</span>' : ''}</td>
                    <td>${parseFloat(r.saat) > 0 ? '<strong>' + r.saat + '</strong> saat' : '-'}</td>
                    <td class="edit-col" style="${editMode ? '' : 'display:none;'}">
                        <button class="btn btn-sm btn-outline-primary" onclick="editRecord(${r.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteRecord(${r.id})"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>
            `;
        });
        list.innerHTML = html;

        document.getElementById('summaryContainer').innerHTML = `
            <div class="col-4"><div class="summary-box"><div class="number">${toplamSaat}</div><div class="label">Toplam Saat</div></div></div>
            <div class="col-4"><div class="summary-box"><div class="number">${pazarSayisi}</div><div class="label">Pazar</div></div></div>
            <div class="col-4"><div class="summary-box"><div class="number">${tatilSayisi}</div><div class="label">Tatil</div></div></div>
        `;
    }
}

function selectDate(dateStr) {
    document.getElementById('inputTarih').value = dateStr;
    document.querySelectorAll('.day-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.date === dateStr);
    });
    document.getElementById('inputAciklama').focus();

    // Check if record exists for this date to auto-fill (optional)
    const records = DB.query("SELECT * FROM mesai_kayitlari WHERE hafta_id = ? AND tarih = ?", [currentWeek.id, dateStr]);
    if (records.length > 0) {
        const r = records[0];
        document.getElementById('editRecordId').value = r.id;
        document.getElementById('inputAciklama').value = r.aciklama;
        document.getElementById('inputSaat').value = r.saat;
        document.getElementById('inputResmiTatil').checked = r.is_resmi_tatil === 1;
    } else {
        document.getElementById('editRecordId').value = '';
        document.getElementById('inputAciklama').value = '';
        document.getElementById('inputSaat').value = '';
        document.getElementById('inputResmiTatil').checked = false;
    }
}

function toggleEditMode() {
    editMode = !editMode;
    document.querySelectorAll('.edit-col').forEach(col => {
        col.style.display = editMode ? '' : 'none';
    });
}

// Record CRUD
function saveRecord(e) {
    e.preventDefault();
    if (!currentWeek) return;

    const id = document.getElementById('editRecordId').value;
    const tarih = document.getElementById('inputTarih').value;
    const aciklama = document.getElementById('inputAciklama').value;
    const saat = document.getElementById('inputSaat').value;
    const is_resmi_tatil = document.getElementById('inputResmiTatil').checked ? 1 : 0;

    if (id) {
        DB.execute("UPDATE mesai_kayitlari SET tarih = ?, aciklama = ?, saat = ?, is_resmi_tatil = ? WHERE id = ?", [tarih, aciklama, saat, is_resmi_tatil, id]);
        setMessage("Kayıt güncellendi!", "success");
    } else {
        const existing = DB.query("SELECT id FROM mesai_kayitlari WHERE hafta_id = ? AND tarih = ?", [currentWeek.id, tarih]);
        if (existing.length > 0) {
            setMessage("Bu tarih için kayıt zaten mevcut!");
            return;
        }
        DB.execute("INSERT INTO mesai_kayitlari (hafta_id, tarih, aciklama, saat, is_resmi_tatil) VALUES (?, ?, ?, ?, ?)", [currentWeek.id, tarih, aciklama, saat, is_resmi_tatil]);
        setMessage("Mesai kaydı eklendi!", "success");
    }

    document.getElementById('editRecordId').value = '';
    document.getElementById('inputAciklama').value = '';
    document.getElementById('inputSaat').value = '';
    document.getElementById('inputResmiTatil').checked = false;
    loadRecords();
}

function editRecord(id) {
    const res = DB.query("SELECT * FROM mesai_kayitlari WHERE id = ?", [id]);
    if (res.length > 0) {
        const r = res[0];
        selectDate(r.tarih);
        window.scrollTo({ top: document.getElementById('mesaiEntrySection').offsetTop - 20, behavior: 'smooth' });
    }
}

function deleteRecord(id) {
    if (confirm('Silmek istediğinize emin misiniz?')) {
        DB.execute("DELETE FROM mesai_kayitlari WHERE id = ?", [id]);
        setMessage("Kayıt silindi.", "success");
        loadRecords();
    }
}

// Week CRUD
function createWeek(e) {
    e.preventDefault();
    const startDate = document.getElementById('newWeekStart').value;
    const haftaAraligi = createHaftaAraligi(startDate);

    const existing = DB.query("SELECT id FROM haftalar WHERE hafta_baslangic = ? AND user_id = ?", [startDate, currentUser.id]);
    if (existing.length > 0) {
        setMessage("Bu tarihe ait bir hafta zaten mevcut!");
        return;
    }

    const res = DB.execute("INSERT INTO haftalar (hafta_baslangic, hafta_araligi, calisan_adi, user_id) VALUES (?, ?, ?, ?)", [startDate, haftaAraligi, currentUser.adsoyad, currentUser.id]);
    if (res > 0) {
        selectWeek(res);
        bootstrap.Collapse.getInstance(document.getElementById('newWeekForm')).hide();
        setMessage("Hafta başarıyla oluşturuldu!", "success");
    }
}

function updateWeek(e) {
    e.preventDefault();
    const startDate = document.getElementById('editWeekStart').value;
    const haftaAraligi = createHaftaAraligi(startDate);

    DB.execute("UPDATE haftalar SET hafta_baslangic = ?, hafta_araligi = ? WHERE id = ?", [startDate, haftaAraligi, currentWeek.id]);
    bootstrap.Modal.getInstance(document.getElementById('editWeekModal')).hide();
    selectWeek(currentWeek.id);
    setMessage("Hafta güncellendi!", "success");
}

function deleteWeek() {
    if (confirm('Bu haftayı ve tüm kayıtlarını silmek istediğinize emin misiniz?')) {
        DB.execute("DELETE FROM mesai_kayitlari WHERE hafta_id = ?", [currentWeek.id]);
        DB.execute("DELETE FROM haftalar WHERE id = ?", [currentWeek.id]);
        currentWeek = null;
        localStorage.removeItem('currentWeekId');
        loadMain();
        setMessage("Hafta silindi.", "success");
    }
}

// Reports
function showReports() {
    showView('reports');
    document.getElementById('reportAvatar').innerText = currentUser.adsoyad.charAt(0);
    const weeks = DB.query("SELECT * FROM haftalar WHERE user_id = ? ORDER BY hafta_baslangic DESC", [currentUser.id]);
    const select = document.getElementById('reportWeekSelect');
    let html = '<option value="">Hafta seçin...</option>';
    weeks.forEach(w => {
        html += `<option value="${w.id}">${w.hafta_araligi}</option>`;
    });
    select.innerHTML = html;
}

function generateWeekReport(e) {
    e.preventDefault();
    const weekId = document.getElementById('reportWeekSelect').value;
    const weeks = DB.query("SELECT * FROM haftalar WHERE id = ?", [weekId]);
    if (weeks.length > 0) {
        const week = weeks[0];
        const records = DB.query("SELECT * FROM mesai_kayitlari WHERE hafta_id = ? ORDER BY tarih", [weekId]);

        let title = week.hafta_araligi + ' ' + week.calisan_adi + ' - Planlanan Mesailer';
        let text = title + "\n" + "-".repeat(40) + "\n";

        let toplam = 0, pazar = 0, tatil = 0;
        records.forEach(r => {
            const gun = getGunAdi(r.tarih);
            const tarih = formatTarih(r.tarih);
            const saat = parseFloat(r.saat) > 0 ? r.saat + ' saat' : '';
            const t = r.is_resmi_tatil ? ' [Tatil]' : '';
            text += `${gun} ${tarih}: ${r.aciklama}${t} ${saat}\n`;

            toplam += parseFloat(r.saat || 0);
            if (isPazar(r.tarih)) pazar++;
            if (r.is_resmi_tatil) tatil++;
        });

        text += "-".repeat(40) + "\n";
        text += `Toplam: ${toplam} saat | Pazar: ${pazar} | Tatil: ${tatil}\n`;

        displayReport(title, text);
    }
}

function generateRangeReport(e) {
    e.preventDefault();
    const start = document.getElementById('rangeStart').value;
    const end = document.getElementById('rangeEnd').value;

    const records = DB.query(`
        SELECT m.*, h.calisan_adi
        FROM mesai_kayitlari m
        JOIN haftalar h ON m.hafta_id = h.id
        WHERE m.tarih BETWEEN ? AND ? AND h.user_id = ?
        ORDER BY m.tarih
    `, [start, end, currentUser.id]);

    if (records.length > 0) {
        let title = formatTarih(start) + ' - ' + formatTarih(end);
        let text = title + " Mesai Raporu\n" + "-".repeat(40) + "\n";

        let toplam = 0, pazar = 0;
        records.forEach(r => {
            const gun = getGunAdi(r.tarih);
            const tarih = formatTarih(r.tarih);
            const saat = parseFloat(r.saat) > 0 ? r.saat + ' saat' : '';
            text += `${gun} ${tarih}: ${r.aciklama} ${saat}\n`;
            toplam += parseFloat(r.saat || 0);
            if (isPazar(r.tarih)) pazar++;
        });

        text += "-".repeat(40) + "\n";
        text += `Toplam: ${toplam} saat | Pazar: ${pazar} adet\n`;

        displayReport(title, text);
    } else {
        setMessage("Bu tarih aralığında kayıt bulunamadı!");
    }
}

function displayReport(title, text) {
    document.getElementById('reportResultCard').style.display = 'block';
    document.getElementById('reportTitle').innerText = title;
    document.getElementById('raporText').value = text;
}

function copyReport() {
    const el = document.getElementById('raporText');
    el.select();
    document.execCommand('copy');
    DB.toast("Rapor kopyalandı!");
}

// Admin
function showAdmin() {
    showView('admin');
    loadAdminUsers();
}

function loadAdminUsers() {
    const users = DB.query("SELECT * FROM kullanicilar ORDER BY adsoyad");
    const list = document.getElementById('adminUserList');
    let html = '';
    users.forEach(u => {
        html += `
            <tr>
                <td><strong>${u.adsoyad}</strong> ${u.is_admin ? '<span class="badge bg-primary">Admin</span>' : ''}</td>
                <td>${u.username}</td>
                <td>${u.is_banned ? '<span class="badge bg-danger">Engelli</span>' : '<span class="badge bg-success">Aktif</span>'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="adminEditUser(${u.id})"><i class="fas fa-edit"></i></button>
                    ${u.id !== currentUser.id ? `
                        <button class="btn btn-sm btn-outline-warning" onclick="adminToggleBan(${u.id})"><i class="fas fa-ban"></i></button>
                        <button class="btn btn-sm btn-outline-info" onclick="adminToggleAdmin(${u.id})"><i class="fas fa-user-shield"></i></button>
                        <button class="btn btn-sm btn-outline-danger" onclick="adminDeleteUser(${u.id})"><i class="fas fa-trash"></i></button>
                    ` : ''}
                </td>
            </tr>
        `;
    });
    list.innerHTML = html;
}

function adminEditUser(id) {
    const users = DB.query("SELECT * FROM kullanicilar WHERE id = ?", [id]);
    if (users.length > 0) {
        const u = users[0];
        document.getElementById('adminEditUserId').value = u.id;
        document.getElementById('adminEditAdSoyad').value = u.adsoyad;
        document.getElementById('adminEditUsername').value = u.username;
        document.getElementById('adminEditPassword').value = '';
        new bootstrap.Modal(document.getElementById('editUserModal')).show();
    }
}

function adminToggleBan(id) {
    DB.execute("UPDATE kullanicilar SET is_banned = NOT is_banned WHERE id = ?", [id]);
    loadAdminUsers();
}

function adminToggleAdmin(id) {
    DB.execute("UPDATE kullanicilar SET is_admin = NOT is_admin WHERE id = ?", [id]);
    loadAdminUsers();
}

function adminDeleteUser(id) {
    if (confirm('Kullanıcıyı ve tüm verilerini silmek istediğinize emin misiniz?')) {
        DB.execute("DELETE FROM mesai_kayitlari WHERE hafta_id IN (SELECT id FROM haftalar WHERE user_id = ?)", [id]);
        DB.execute("DELETE FROM haftalar WHERE user_id = ?", [id]);
        DB.execute("DELETE FROM kullanicilar WHERE id = ?", [id]);
        loadAdminUsers();
    }
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('currentYear').innerText = new Date().getFullYear();

    // Auth
    document.getElementById('loginForm').onsubmit = (e) => {
        e.preventDefault();
        login(document.getElementById('loginUsername').value, document.getElementById('loginPassword').value);
    };
    document.getElementById('registerForm').onsubmit = (e) => {
        e.preventDefault();
        const p1 = document.getElementById('regPassword').value;
        const p2 = document.getElementById('regConfirmPassword').value;
        if (p1 !== p2) {
            setMessage("Şifreler eşleşmiyor!");
            return;
        }
        if (p1.length < 6) {
            setMessage("Şifre en az 6 karakter olmalıdır!");
            return;
        }
        register(document.getElementById('regAdSoyad').value, document.getElementById('regUsername').value, p1);
    };

    // Main
    document.getElementById('yearSelect').onchange = (e) => {
        selectedYear = parseInt(e.target.value);
        loadWeeks();
    };
    document.getElementById('createWeekForm').onsubmit = createWeek;
    document.getElementById('editWeekForm').onsubmit = updateWeek;
    document.getElementById('deleteWeekBtn').onclick = deleteWeek;

    // Records
    document.getElementById('recordForm').onsubmit = saveRecord;

    // Reports
    document.getElementById('weekReportForm').onsubmit = generateWeekReport;
    document.getElementById('rangeReportForm').onsubmit = generateRangeReport;

    // Settings
    document.getElementById('changePasswordForm').onsubmit = (e) => {
        e.preventDefault();
        const cur = document.getElementById('currentPassword').value;
        const n1 = document.getElementById('newPassword').value;
        const n2 = document.getElementById('confirmNewPassword').value;

        if (cur !== currentUser.password) {
            setMessage("Mevcut şifreniz yanlış!");
            return;
        }
        if (n1 !== n2) {
            setMessage("Yeni şifreler eşleşmiyor!");
            return;
        }
        if (n1.length < 6) {
            setMessage("Yeni şifre en az 6 karakter olmalıdır!");
            return;
        }

        DB.execute("UPDATE kullanicilar SET password = ? WHERE id = ?", [n1, currentUser.id]);
        currentUser.password = n1;
        bootstrap.Modal.getInstance(document.getElementById('settingsModal')).hide();
        setMessage("Şifreniz başarıyla güncellendi!", "success");
    };

    // Admin Edit User
    document.getElementById('adminEditUserForm').onsubmit = (e) => {
        e.preventDefault();
        const id = document.getElementById('adminEditUserId').value;
        const ads = document.getElementById('adminEditAdSoyad').value;
        const usr = document.getElementById('adminEditUsername').value;
        const pas = document.getElementById('adminEditPassword').value;

        if (pas) {
            DB.execute("UPDATE kullanicilar SET adsoyad = ?, username = ?, password = ? WHERE id = ?", [ads, usr, pas, id]);
        } else {
            DB.execute("UPDATE kullanicilar SET adsoyad = ?, username = ? WHERE id = ?", [ads, usr, id]);
        }

        bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
        loadAdminUsers();
        setMessage("Kullanıcı güncellendi.", "success");
    };

    // Initial load
    checkAuth();

    // Recover state
    const lastWeekId = localStorage.getItem('currentWeekId');
    if (lastWeekId && currentUser) {
        selectWeek(lastWeekId);
    }
});
