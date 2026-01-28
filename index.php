<?php
session_start();

// ==================== YAPILANDIRMA ====================
$db_host = 'fdb1028.awardspace.net';
$db_name = '4308587_aegeans';
$db_user = '4308587_aegeans';
$db_password = 'Aeg151851';

// ==================== YARDIMCI FONKSİYONLAR ====================

function getDb() {
    global $db_host, $db_name, $db_user, $db_password;
    static $db = null;
    if ($db === null) {
        try {
            $db = new PDO("mysql:host=$db_host;dbname=$db_name;charset=utf8mb4", $db_user, $db_password);
            $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $db->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } catch (PDOException $e) {
            die("Veritabanı bağlantı hatası: " . $e->getMessage());
        }
    }
    return $db;
}

function checkLogin() {
    return isset($_SESSION['logged_in']) && $_SESSION['logged_in'] === true;
}

function isAdmin() {
    return checkLogin() && isset($_SESSION['is_admin']) && $_SESSION['is_admin'] === 1;
}

function formatTarih($date) {
    if (empty($date)) return '';
    $aylar = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
              'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
    try {
        $d = new DateTime($date);
        return $d->format('j') . ' ' . $aylar[$d->format('n') - 1];
    } catch (Exception $e) {
        return $date;
    }
}

function getGunAdi($date, $kisalt = true) {
    if (empty($date)) return '';
    try {
        $d = new DateTime($date);
        $gunler_kisa = ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'];
        $gunler_uzun = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
        return $kisalt ? $gunler_kisa[$d->format('w')] : $gunler_uzun[$d->format('w')];
    } catch (Exception $e) {
        return '';
    }
}

function isPazar($date) {
    if (empty($date)) return false;
    try {
        $d = new DateTime($date);
        return $d->format('w') == 0;
    } catch (Exception $e) {
        return false;
    }
}

function isHaftaSonu($date) {
    if (empty($date)) return false;
    try {
        $d = new DateTime($date);
        $gun = $d->format('w');
        return $gun == 0 || $gun == 6;
    } catch (Exception $e) {
        return false;
    }
}

function createHaftaAraligi($startDate) {
    try {
        $start = new DateTime($startDate);
        $end = clone $start;
        $end->modify('+6 days');
        
        $aylar = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
                  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
        
        if ($start->format('n') === $end->format('n')) {
            return $start->format('j') . '-' . $end->format('j') . ' ' . $aylar[$start->format('n') - 1];
        }
        return $start->format('j') . ' ' . $aylar[$start->format('n') - 1] . ' - ' . 
               $end->format('j') . ' ' . $aylar[$end->format('n') - 1];
    } catch (Exception $e) {
        return '';
    }
}

function setMessage($message, $type = 'danger') {
    $_SESSION['flash_message'] = ['text' => $message, 'type' => $type];
}

function getMessage() {
    if (isset($_SESSION['flash_message'])) {
        $msg = $_SESSION['flash_message'];
        unset($_SESSION['flash_message']);
        return $msg;
    }
    return null;
}

// ==================== VERİTABANI BAĞLANTISI ====================
$db = getDb();

// ==================== YEDEKLEME İŞLEMLERİ ====================

if (isset($_GET['backup_db']) && isAdmin()) {
    try {
        $tables = $db->query("SHOW TABLES")->fetchAll(PDO::FETCH_COLUMN);
        $backup = "-- Mesai Takip Sistemi Yedeği\n-- Tarih: " . date('Y-m-d H:i:s') . "\n\n";
        
        foreach ($tables as $table) {
            $create = $db->query("SHOW CREATE TABLE `$table`")->fetch();
            $backup .= "DROP TABLE IF EXISTS `$table`;\n";
            $backup .= $create['Create Table'] . ";\n\n";
            
            $rows = $db->query("SELECT * FROM `$table`")->fetchAll();
            foreach ($rows as $row) {
                $values = array_map(function($v) use ($db) {
                    return $v === null ? 'NULL' : $db->quote($v);
                }, $row);
                $backup .= "INSERT INTO `$table` VALUES (" . implode(', ', $values) . ");\n";
            }
            $backup .= "\n";
        }
        
        header('Content-Type: application/sql');
        header('Content-Disposition: attachment; filename="mesai_backup_' . date("Y-m-d_H-i-s") . '.sql"');
        echo $backup;
        exit;
    } catch (Exception $e) {
        setMessage("Yedek alınırken hata: " . $e->getMessage());
    }
}

if (isset($_POST['restore_db']) && isAdmin()) {
    if (isset($_FILES['backup_file']) && $_FILES['backup_file']['error'] == 0) {
        try {
            $sql = file_get_contents($_FILES['backup_file']['tmp_name']);
            $dbRestore = new PDO("mysql:host=$db_host;dbname=$db_name;charset=utf8mb4", $db_user, $db_password, 
                                 [PDO::MYSQL_ATTR_MULTI_STATEMENTS => true]);
            $dbRestore->exec($sql);
            setMessage("Veritabanı başarıyla geri yüklendi.", 'success');
        } catch (PDOException $e) {
            setMessage("Yedek geri yüklenirken hata: " . $e->getMessage());
        }
    } else {
        setMessage("Yedek dosyası yüklenemedi.");
    }
    header('Location: ' . $_SERVER['PHP_SELF'] . '?view=admin');
    exit;
}

// ==================== KULLANICI İŞLEMLERİ ====================

if (isset($_POST['register'])) {
    $username = trim($_POST['reg_username'] ?? '');
    $password = $_POST['reg_password'] ?? '';
    $confirm_password = $_POST['reg_confirm_password'] ?? '';
    $adsoyad = trim($_POST['reg_adsoyad'] ?? '');
    
    if (empty($username) || empty($password) || empty($confirm_password) || empty($adsoyad)) {
        setMessage("Tüm alanlar doldurulmalıdır!");
    } elseif (strlen($password) < 6) {
        setMessage("Şifre en az 6 karakter olmalıdır!");
    } elseif ($password !== $confirm_password) {
        setMessage("Şifreler eşleşmiyor!");
    } else {
        $stmt = $db->prepare("SELECT id FROM kullanicilar WHERE username = ?");
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            setMessage("Bu kullanıcı adı zaten kullanılıyor!");
        } else {
            $hashed = password_hash($password, PASSWORD_DEFAULT);
            $stmt = $db->prepare("INSERT INTO kullanicilar (username, password, adsoyad, is_admin) VALUES (?, ?, ?, 0)");
            $stmt->execute([$username, $hashed, $adsoyad]);
            
            $_SESSION['logged_in'] = true;
            $_SESSION['user_id'] = $db->lastInsertId();
            $_SESSION['username'] = $username;
            $_SESSION['adsoyad'] = $adsoyad;
            $_SESSION['is_admin'] = 0;
            
            setMessage("Kayıt başarılı! Hoşgeldiniz.", 'success');
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
    }
}

if (isset($_POST['login'])) {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    $remember = isset($_POST['remember']);
    
    $stmt = $db->prepare("SELECT * FROM kullanicilar WHERE username = ?");
    $stmt->execute([$username]);
    $user = $stmt->fetch();
    
    if ($user && password_verify($password, $user['password'])) {
        if (!empty($user['is_banned'])) {
            setMessage("Bu hesap engellenmiştir.");
        } else {
            $_SESSION['logged_in'] = true;
            $_SESSION['user_id'] = $user['id'];
            $_SESSION['username'] = $user['username'];
            $_SESSION['adsoyad'] = $user['adsoyad'];
            $_SESSION['is_admin'] = $user['is_admin'];
            
            if ($remember) {
                setcookie('remember_user', $username, time() + (86400 * 30), "/", "", false, true);
            }
            
            header('Location: ' . $_SERVER['PHP_SELF']);
            exit;
        }
    } else {
        setMessage('Hatalı kullanıcı adı veya şifre!');
    }
}

if (isset($_GET['logout'])) {
    session_destroy();
    setcookie('remember_user', '', time() - 3600, "/");
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

if (isset($_POST['change_password']) && checkLogin()) {
    $current = $_POST['current_password'] ?? '';
    $new = $_POST['new_password'] ?? '';
    $confirm = $_POST['confirm_new_password'] ?? '';
    
    $stmt = $db->prepare("SELECT password FROM kullanicilar WHERE id = ?");
    $stmt->execute([$_SESSION['user_id']]);
    $user = $stmt->fetch();
    
    if (!$user || !password_verify($current, $user['password'])) {
        setMessage("Mevcut şifreniz yanlış!");
    } elseif (strlen($new) < 6) {
        setMessage("Yeni şifre en az 6 karakter olmalıdır!");
    } elseif ($new !== $confirm) {
        setMessage("Yeni şifreler eşleşmiyor!");
    } else {
        $stmt = $db->prepare("UPDATE kullanicilar SET password = ? WHERE id = ?");
        $stmt->execute([password_hash($new, PASSWORD_DEFAULT), $_SESSION['user_id']]);
        setMessage("Şifreniz başarıyla güncellendi!", 'success');
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

// ==================== HAFTA İŞLEMLERİ ====================

if (isset($_POST['create_week']) && checkLogin()) {
    $haftaBaslangic = $_POST['hafta_baslangic'] ?? '';
    
    if (empty($haftaBaslangic)) {
        setMessage("Hafta başlangıç tarihi gereklidir!");
    } else {
        $haftaAraligi = createHaftaAraligi($haftaBaslangic);
        
        $stmt = $db->prepare("SELECT id FROM haftalar WHERE hafta_baslangic = ? AND user_id = ?");
        $stmt->execute([$haftaBaslangic, $_SESSION['user_id']]);
        if ($stmt->fetch()) {
            setMessage("Bu tarihe ait bir hafta zaten mevcut!");
        } else {
            $stmt = $db->prepare("INSERT INTO haftalar (hafta_baslangic, hafta_araligi, calisan_adi, user_id) VALUES (?, ?, ?, ?)");
            $stmt->execute([$haftaBaslangic, $haftaAraligi, $_SESSION['adsoyad'], $_SESSION['user_id']]);
            $_SESSION['current_week'] = $db->lastInsertId();
            setMessage("Hafta başarıyla oluşturuldu!", 'success');
        }
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

if (isset($_GET['select_week']) && checkLogin()) {
    $week_id = (int)$_GET['select_week'];
    $stmt = $db->prepare("SELECT id FROM haftalar WHERE id = ? AND user_id = ?");
    $stmt->execute([$week_id, $_SESSION['user_id']]);
    if ($stmt->fetch()) {
        $_SESSION['current_week'] = $week_id;
    }
    header('Location: ' . $_SERVER['PHP_SELF'] . (isset($_GET['year']) ? '?year=' . (int)$_GET['year'] : ''));
    exit;
}

if (isset($_POST['delete_week']) && checkLogin()) {
    $week_id = (int)($_POST['week_id'] ?? 0);
    
    $stmt = $db->prepare("SELECT id FROM haftalar WHERE id = ? AND user_id = ?");
    $stmt->execute([$week_id, $_SESSION['user_id']]);
    
    if ($stmt->fetch()) {
        $db->prepare("DELETE FROM mesai_kayitlari WHERE hafta_id = ?")->execute([$week_id]);
        $db->prepare("DELETE FROM haftalar WHERE id = ?")->execute([$week_id]);
        
        if (isset($_SESSION['current_week']) && $_SESSION['current_week'] == $week_id) {
            unset($_SESSION['current_week']);
        }
        setMessage("Hafta silindi.", 'success');
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

if (isset($_POST['edit_week']) && checkLogin()) {
    $week_id = (int)($_POST['week_id'] ?? 0);
    $hafta_baslangic = $_POST['hafta_baslangic'] ?? '';
    
    $stmt = $db->prepare("SELECT id FROM haftalar WHERE id = ? AND user_id = ?");
    $stmt->execute([$week_id, $_SESSION['user_id']]);
    
    if ($stmt->fetch() && !empty($hafta_baslangic)) {
        $haftaAraligi = createHaftaAraligi($hafta_baslangic);
        $stmt = $db->prepare("UPDATE haftalar SET hafta_baslangic = ?, hafta_araligi = ? WHERE id = ?");
        $stmt->execute([$hafta_baslangic, $haftaAraligi, $week_id]);
        setMessage("Hafta güncellendi!", 'success');
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

// ==================== MESAİ KAYIT İŞLEMLERİ ====================

if (isset($_POST['submit']) && checkLogin() && isset($_SESSION['current_week'])) {
    $tarih = $_POST['tarih'] ?? '';
    $aciklama = trim($_POST['aciklama'] ?? '');
    $saat = trim($_POST['saat'] ?? '');
    $is_resmi_tatil = isset($_POST['is_resmi_tatil']) ? 1 : 0;
    
    $stmt = $db->prepare("SELECT hafta_baslangic FROM haftalar WHERE id = ? AND user_id = ?");
    $stmt->execute([$_SESSION['current_week'], $_SESSION['user_id']]);
    $hafta = $stmt->fetch();
    
    if (!$hafta) {
        setMessage("Hafta bulunamadı!");
    } elseif (empty($tarih)) {
        setMessage("Tarih seçiniz!");
    } elseif (empty($aciklama)) {
        setMessage("Açıklama giriniz!");
    } else {
        $weekStart = new DateTime($hafta['hafta_baslangic']);
        $weekEnd = clone $weekStart;
        $weekEnd->modify('+6 days');
        $enteredDate = new DateTime($tarih);
        
        if ($enteredDate < $weekStart || $enteredDate > $weekEnd) {
            setMessage("Tarih seçili hafta içinde olmalıdır!");
        } else {
            $isPazarGun = isPazar($tarih);
            if ($saat === '' && !$is_resmi_tatil && !$isPazarGun) {
                setMessage("Saat alanı zorunludur!");
            } else {
                $stmt = $db->prepare("SELECT id FROM mesai_kayitlari WHERE hafta_id = ? AND tarih = ?");
                $stmt->execute([$_SESSION['current_week'], $tarih]);
                
                if ($stmt->fetch()) {
                    setMessage("Bu tarih için kayıt zaten mevcut!");
                } else {
                    $saatKayit = ($saat === '' && ($isPazarGun || $is_resmi_tatil)) ? '0' : $saat;
                    $stmt = $db->prepare("INSERT INTO mesai_kayitlari (hafta_id, tarih, aciklama, saat, is_resmi_tatil) VALUES (?, ?, ?, ?, ?)");
                    $stmt->execute([$_SESSION['current_week'], $tarih, $aciklama, $saatKayit, $is_resmi_tatil]);
                    setMessage("Mesai kaydı eklendi!", 'success');
                }
            }
        }
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

if (isset($_POST['edit']) && checkLogin() && isset($_SESSION['current_week'])) {
    $id = (int)($_POST['id'] ?? 0);
    $tarih = $_POST['tarih'] ?? '';
    $aciklama = trim($_POST['aciklama'] ?? '');
    $saat = trim($_POST['saat'] ?? '');
    $is_resmi_tatil = isset($_POST['is_resmi_tatil']) ? 1 : 0;
    
    $stmt = $db->prepare("SELECT mk.id FROM mesai_kayitlari mk JOIN haftalar h ON mk.hafta_id = h.id WHERE mk.id = ? AND h.user_id = ?");
    $stmt->execute([$id, $_SESSION['user_id']]);
    
    if (!$stmt->fetch()) {
        setMessage("Yetkisiz işlem!");
    } elseif (empty($aciklama)) {
        setMessage("Açıklama giriniz!");
    } else {
        $stmt = $db->prepare("SELECT id FROM mesai_kayitlari WHERE hafta_id = ? AND tarih = ? AND id != ?");
        $stmt->execute([$_SESSION['current_week'], $tarih, $id]);
        
        if ($stmt->fetch()) {
            setMessage("Bu tarih için başka bir kayıt mevcut!");
        } else {
            $isPazarGun = isPazar($tarih);
            $saatKayit = ($saat === '' && ($isPazarGun || $is_resmi_tatil)) ? '0' : $saat;
            
            $stmt = $db->prepare("UPDATE mesai_kayitlari SET tarih = ?, aciklama = ?, saat = ?, is_resmi_tatil = ? WHERE id = ?");
            $stmt->execute([$tarih, $aciklama, $saatKayit, $is_resmi_tatil, $id]);
            setMessage("Kayıt güncellendi!", 'success');
        }
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

if (isset($_POST['delete_record']) && checkLogin()) {
    $id = (int)($_POST['record_id'] ?? 0);
    
    $stmt = $db->prepare("SELECT mk.id FROM mesai_kayitlari mk JOIN haftalar h ON mk.hafta_id = h.id WHERE mk.id = ? AND h.user_id = ?");
    $stmt->execute([$id, $_SESSION['user_id']]);
    
    if ($stmt->fetch()) {
        $db->prepare("DELETE FROM mesai_kayitlari WHERE id = ?")->execute([$id]);
        setMessage("Kayıt silindi.", 'success');
    }
    header('Location: ' . $_SERVER['PHP_SELF']);
    exit;
}

// ==================== ADMİN İŞLEMLERİ ====================

if (isAdmin()) {
    if (isset($_GET['delete_user'])) {
        $user_id = (int)$_GET['delete_user'];
        if ($user_id != $_SESSION['user_id']) {
            $db->prepare("DELETE FROM mesai_kayitlari WHERE hafta_id IN (SELECT id FROM haftalar WHERE user_id = ?)")->execute([$user_id]);
            $db->prepare("DELETE FROM haftalar WHERE user_id = ?")->execute([$user_id]);
            $db->prepare("DELETE FROM kullanicilar WHERE id = ?")->execute([$user_id]);
            setMessage("Kullanıcı silindi.", 'success');
        }
        header('Location: ' . $_SERVER['PHP_SELF'] . '?view=admin');
        exit;
    }
    
    if (isset($_GET['toggle_ban'])) {
        $user_id = (int)$_GET['toggle_ban'];
        if ($user_id != $_SESSION['user_id']) {
            $db->prepare("UPDATE kullanicilar SET is_banned = NOT is_banned WHERE id = ?")->execute([$user_id]);
            setMessage("Ban durumu güncellendi.", 'success');
        }
        header('Location: ' . $_SERVER['PHP_SELF'] . '?view=admin');
        exit;
    }
    
    if (isset($_GET['toggle_admin'])) {
        $user_id = (int)$_GET['toggle_admin'];
        if ($user_id != $_SESSION['user_id']) {
            $db->prepare("UPDATE kullanicilar SET is_admin = NOT is_admin WHERE id = ?")->execute([$user_id]);
            setMessage("Admin yetkisi güncellendi.", 'success');
        }
        header('Location: ' . $_SERVER['PHP_SELF'] . '?view=admin');
        exit;
    }
    
    if (isset($_POST['edit_user'])) {
        $user_id = (int)$_POST['user_id'];
        $new_username = trim($_POST['new_username']);
        $new_adsoyad = trim($_POST['new_adsoyad']);
        $new_password = $_POST['new_password'] ?? '';
        
        if (!empty($new_password)) {
            $hashed = password_hash($new_password, PASSWORD_DEFAULT);
            $stmt = $db->prepare("UPDATE kullanicilar SET username = ?, password = ?, adsoyad = ? WHERE id = ?");
            $stmt->execute([$new_username, $hashed, $new_adsoyad, $user_id]);
        } else {
            $stmt = $db->prepare("UPDATE kullanicilar SET username = ?, adsoyad = ? WHERE id = ?");
            $stmt->execute([$new_username, $new_adsoyad, $user_id]);
        }
        setMessage("Kullanıcı güncellendi.", 'success');
        header('Location: ' . $_SERVER['PHP_SELF'] . '?view=admin');
        exit;
    }
}

// ==================== RAPOR OLUŞTURMA ====================

$rapor_text = '';
$rapor_title = '';

if (isset($_POST['generate_week_report']) && checkLogin()) {
    $week_id = (int)$_POST['selected_week'];
    
    $stmt = $db->prepare("SELECT * FROM haftalar WHERE id = ? AND user_id = ?");
    $stmt->execute([$week_id, $_SESSION['user_id']]);
    $week = $stmt->fetch();
    
    if ($week) {
        $stmt = $db->prepare("SELECT * FROM mesai_kayitlari WHERE hafta_id = ? ORDER BY tarih");
        $stmt->execute([$week_id]);
        $kayitlar = $stmt->fetchAll();
        
        if (!empty($kayitlar)) {
            $rapor_title = $week['hafta_araligi'] . ' ' . $week['calisan_adi'] . ' - Planlanan Mesailer';
            $rapor_text = $rapor_title . "\n";
            $rapor_text .= str_repeat('-', 40) . "\n";
            
            $toplam = 0;
            $pazar_say = 0;
            $tatil_say = 0;
            
            foreach ($kayitlar as $k) {
                $gun = getGunAdi($k['tarih']);
                $tarih = formatTarih($k['tarih']);
                $saat = (float)$k['saat'] > 0 ? $k['saat'] . ' saat' : '';
                $tatil = $k['is_resmi_tatil'] ? ' [Tatil]' : '';
                
                $rapor_text .= "$gun $tarih: {$k['aciklama']}$tatil $saat\n";
                
                $toplam += (float)$k['saat'];
                if (isPazar($k['tarih'])) $pazar_say++;
                if ($k['is_resmi_tatil']) $tatil_say++;
            }
            
            $rapor_text .= str_repeat('-', 40) . "\n";
            $rapor_text .= "Toplam: $toplam saat | Pazar: $pazar_say | Tatil: $tatil_say\n";
        } else {
            setMessage("Bu haftada kayıt bulunamadı!");
        }
    }
}

if (isset($_POST['generate_date_range_report']) && checkLogin()) {
    $baslangic = $_POST['baslangic_tarihi'] ?? '';
    $bitis = $_POST['bitis_tarihi'] ?? '';
    
    if (empty($baslangic) || empty($bitis)) {
        setMessage("Tarih aralığı seçiniz!");
    } elseif ($baslangic > $bitis) {
        setMessage("Başlangıç tarihi bitiş tarihinden önce olmalıdır!");
    } else {
        $stmt = $db->prepare("
            SELECT m.*, h.calisan_adi 
            FROM mesai_kayitlari m
            JOIN haftalar h ON m.hafta_id = h.id
            WHERE m.tarih BETWEEN ? AND ? AND h.user_id = ?
            ORDER BY m.tarih
        ");
        $stmt->execute([$baslangic, $bitis, $_SESSION['user_id']]);
        $kayitlar = $stmt->fetchAll();
        
        if (!empty($kayitlar)) {
            $rapor_title = formatTarih($baslangic) . ' - ' . formatTarih($bitis);
            $rapor_text = $rapor_title . " Mesai Raporu\n";
            $rapor_text .= str_repeat('-', 40) . "\n";
            
            $toplam = 0;
            $pazar_say = 0;
            
            foreach ($kayitlar as $k) {
                $gun = getGunAdi($k['tarih']);
                $tarih = formatTarih($k['tarih']);
                $saat = (float)$k['saat'] > 0 ? $k['saat'] . ' saat' : '';
                
                $rapor_text .= "$gun $tarih: {$k['aciklama']} $saat\n";
                $toplam += (float)$k['saat'];
                if (isPazar($k['tarih'])) $pazar_say++;
            }
            
            $rapor_text .= str_repeat('-', 40) . "\n";
            $rapor_text .= "Toplam: $toplam saat | Pazar: $pazar_say adet\n";
        } else {
            setMessage("Bu tarih aralığında kayıt bulunamadı!");
        }
    }
}

// ==================== VERİ HAZIRLAMA ====================

$currentWeek = null;
$mesaiKayitlari = [];
$allWeeks = [];
$yearOptions = [];
$selectedYear = (int)date('Y');

if (checkLogin()) {
    if (isset($_SESSION['current_week'])) {
        $stmt = $db->prepare("SELECT * FROM haftalar WHERE id = ? AND user_id = ?");
        $stmt->execute([$_SESSION['current_week'], $_SESSION['user_id']]);
        $currentWeek = $stmt->fetch();
        
        if ($currentWeek) {
            $stmt = $db->prepare("SELECT * FROM mesai_kayitlari WHERE hafta_id = ? ORDER BY tarih");
            $stmt->execute([$_SESSION['current_week']]);
            $mesaiKayitlari = $stmt->fetchAll();
        }
    }
    
    $stmt = $db->prepare("SELECT DISTINCT YEAR(hafta_baslangic) as yil FROM haftalar WHERE user_id = ? ORDER BY yil DESC");
    $stmt->execute([$_SESSION['user_id']]);
    $yearOptions = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    if (!in_array(date('Y'), $yearOptions)) {
        array_unshift($yearOptions, (int)date('Y'));
    }
    
    $selectedYear = isset($_GET['year']) ? (int)$_GET['year'] : (int)date('Y');
    
    $stmt = $db->prepare("SELECT * FROM haftalar WHERE user_id = ? AND YEAR(hafta_baslangic) = ? ORDER BY hafta_baslangic DESC");
    $stmt->execute([$_SESSION['user_id'], $selectedYear]);
    $allWeeks = $stmt->fetchAll();
}

$toplam_saat = 0;
$pazar_sayisi = 0;
$resmi_tatil_sayisi = 0;

foreach ($mesaiKayitlari as $item) {
    $toplam_saat += (float)$item['saat'];
    if (isPazar($item['tarih'])) $pazar_sayisi++;
    if ($item['is_resmi_tatil']) $resmi_tatil_sayisi++;
}

$flashMessage = getMessage();
?>
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mesai Takip</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        :root { --primary: #4361ee; --success: #10b981; }
        body { font-family: 'Segoe UI', sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding-bottom: 60px; }
        .main-container { max-width: 900px; margin: 0 auto; padding: 15px; }
        .card { border: none; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); margin-bottom: 20px; }
        .card-header { background: var(--primary); color: white; font-weight: 600; padding: 15px 20px; border: none; border-radius: 16px 16px 0 0 !important; }
        .btn { border-radius: 10px; padding: 10px 20px; font-weight: 500; }
        .btn-primary { background: var(--primary); border: none; }
        .btn-success { background: var(--success); border: none; }
        .day-btn { padding: 12px 8px; border-radius: 12px; text-align: center; cursor: pointer; border: 2px solid #dee2e6; background: white; min-width: 70px; flex: 1; transition: all 0.2s; }
        .day-btn:hover { border-color: var(--primary); background: #f0f4ff; }
        .day-btn.active { background: var(--primary); color: white; border-color: var(--primary); }
        .day-btn.filled { background: #e8f5e9; border-color: #4caf50; }
        .day-btn.weekend { background: #fff3e0; border-color: #ff9800; }
        .day-btn .day-name { font-weight: 700; font-size: 0.85rem; }
        .day-btn .day-date { font-size: 0.75rem; opacity: 0.8; }
        .top-nav { background: rgba(255,255,255,0.95); border-radius: 16px; padding: 15px 20px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
        .user-avatar { width: 40px; height: 40px; background: var(--primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; }
        .summary-box { background: white; border-radius: 12px; padding: 15px; text-align: center; }
        .summary-box .number { font-size: 1.8rem; font-weight: 700; color: var(--primary); }
        .summary-box .label { font-size: 0.8rem; color: #6c757d; }
        .row-pazar { background: #fff8e1 !important; }
        .row-tatil { background: #e3f2fd !important; }
        .form-control, .form-select { border-radius: 10px; padding: 12px 15px; border: 2px solid #e9ecef; }
        .form-control:focus, .form-select:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(67,97,238,0.1); }
        footer { position: fixed; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.8); color: white; text-align: center; padding: 10px; font-size: 0.8rem; }
        .steps { display: flex; justify-content: center; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
        .step { display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: rgba(255,255,255,0.3); border-radius: 20px; font-size: 0.85rem; color: white; }
        .step.active { background: white; color: var(--primary); font-weight: 600; }
        @media (max-width: 768px) { .day-btn { min-width: 45px; padding: 8px 4px; } .day-btn .day-name { font-size: 0.7rem; } .day-btn .day-date { font-size: 0.6rem; } }
    </style>
</head>
<body>
<div class="main-container">
    
    <?php if ($flashMessage): ?>
    <div class="alert alert-<?php echo $flashMessage['type']; ?> alert-dismissible fade show">
        <?php echo htmlspecialchars($flashMessage['text']); ?>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <?php endif; ?>
    
    <?php if (!checkLogin()): ?>
    <div class="text-center text-white mb-4">
        <h1><i class="fas fa-clock"></i> Mesai Takip</h1>
        <p>Mesai kayıtlarınızı kolayca yönetin</p>
    </div>
    
    <div class="card">
        <div class="card-body">
            <ul class="nav nav-pills nav-justified mb-4">
                <li class="nav-item">
                    <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#loginTab">
                        <i class="fas fa-sign-in-alt"></i> Giriş
                    </button>
                </li>
                <li class="nav-item">
                    <button class="nav-link" data-bs-toggle="tab" data-bs-target="#registerTab">
                        <i class="fas fa-user-plus"></i> Kayıt
                    </button>
                </li>
            </ul>
            
            <div class="tab-content">
                <div class="tab-pane fade show active" id="loginTab">
                    <form method="post">
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-user"></i> Kullanıcı Adı</label>
                            <input type="text" name="username" class="form-control" value="<?php echo htmlspecialchars($_COOKIE['remember_user'] ?? ''); ?>" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-lock"></i> Şifre</label>
                            <input type="password" name="password" class="form-control" required>
                        </div>
                        <div class="mb-3 form-check">
                            <input type="checkbox" name="remember" class="form-check-input" id="remember">
                            <label class="form-check-label" for="remember">Beni hatırla</label>
                        </div>
                        <button type="submit" name="login" class="btn btn-primary w-100">
                            <i class="fas fa-sign-in-alt"></i> Giriş Yap
                        </button>
                    </form>
                </div>
                
                <div class="tab-pane fade" id="registerTab">
                    <form method="post">
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-id-card"></i> Ad Soyad</label>
                            <input type="text" name="reg_adsoyad" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-user"></i> Kullanıcı Adı</label>
                            <input type="text" name="reg_username" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-lock"></i> Şifre</label>
                            <input type="password" name="reg_password" class="form-control" required>
                            <small class="text-muted">En az 6 karakter</small>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><i class="fas fa-lock"></i> Şifre Tekrar</label>
                            <input type="password" name="reg_confirm_password" class="form-control" required>
                        </div>
                        <button type="submit" name="register" class="btn btn-success w-100">
                            <i class="fas fa-user-plus"></i> Kayıt Ol
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <?php elseif (isset($_GET['view']) && $_GET['view'] === 'admin' && isAdmin()): ?>
    <div class="top-nav">
        <div class="d-flex align-items-center gap-2">
            <div class="user-avatar"><i class="fas fa-shield-alt"></i></div>
            <strong>Admin Paneli</strong>
        </div>
        <div>
            <a href="<?php echo $_SERVER['PHP_SELF']; ?>" class="btn btn-outline-primary btn-sm"><i class="fas fa-arrow-left"></i> Geri</a>
            <a href="?logout" class="btn btn-outline-danger btn-sm"><i class="fas fa-sign-out-alt"></i></a>
        </div>
    </div>
    
    <div class="card">
        <div class="card-header"><i class="fas fa-database"></i> Yedekleme</div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-6">
                    <a href="?backup_db=1" class="btn btn-success w-100"><i class="fas fa-download"></i> Yedek İndir</a>
                </div>
                <div class="col-md-6">
                    <form method="post" enctype="multipart/form-data" class="d-flex gap-2">
                        <input type="file" name="backup_file" class="form-control" accept=".sql" required>
                        <button type="submit" name="restore_db" class="btn btn-warning" onclick="return confirm('Emin misiniz?')"><i class="fas fa-upload"></i></button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <div class="card">
        <div class="card-header"><i class="fas fa-users"></i> Kullanıcılar</div>
        <div class="card-body">
            <input type="text" id="userSearch" class="form-control mb-3" placeholder="🔍 Ara...">
            <div class="table-responsive">
                <table class="table" id="userTable">
                    <thead><tr><th>Ad Soyad</th><th>Kullanıcı</th><th>Durum</th><th>İşlem</th></tr></thead>
                    <tbody>
                        <?php
                        $users = $db->query("SELECT * FROM kullanicilar ORDER BY adsoyad")->fetchAll();
                        foreach ($users as $u):
                        ?>
                        <tr>
                            <td><strong><?php echo htmlspecialchars($u['adsoyad']); ?></strong>
                                <?php if ($u['is_admin']): ?><span class="badge bg-primary">Admin</span><?php endif; ?>
                            </td>
                            <td><?php echo htmlspecialchars($u['username']); ?></td>
                            <td><?php echo $u['is_banned'] ? '<span class="badge bg-danger">Engelli</span>' : '<span class="badge bg-success">Aktif</span>'; ?></td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editUser<?php echo $u['id']; ?>"><i class="fas fa-edit"></i></button>
                                <?php if ($u['id'] != $_SESSION['user_id']): ?>
                                <a href="?toggle_ban=<?php echo $u['id']; ?>&view=admin" class="btn btn-sm btn-outline-warning"><i class="fas fa-ban"></i></a>
                                <a href="?toggle_admin=<?php echo $u['id']; ?>&view=admin" class="btn btn-sm btn-outline-info"><i class="fas fa-user-shield"></i></a>
                                <a href="?delete_user=<?php echo $u['id']; ?>&view=admin" class="btn btn-sm btn-outline-danger" onclick="return confirm('Silmek istediğinize emin misiniz?')"><i class="fas fa-trash"></i></a>
                                <?php endif; ?>
                            </td>
                        </tr>
                        <div class="modal fade" id="editUser<?php echo $u['id']; ?>"><div class="modal-dialog"><div class="modal-content">
                            <form method="post">
                                <div class="modal-header"><h5>Düzenle</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                                <div class="modal-body">
                                    <input type="hidden" name="user_id" value="<?php echo $u['id']; ?>">
                                    <div class="mb-3"><label>Ad Soyad</label><input type="text" name="new_adsoyad" class="form-control" value="<?php echo htmlspecialchars($u['adsoyad']); ?>" required></div>
                                    <div class="mb-3"><label>Kullanıcı Adı</label><input type="text" name="new_username" class="form-control" value="<?php echo htmlspecialchars($u['username']); ?>" required></div>
                                    <div class="mb-3"><label>Yeni Şifre</label><input type="password" name="new_password" class="form-control"></div>
                                </div>
                                <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">İptal</button><button type="submit" name="edit_user" class="btn btn-primary">Kaydet</button></div>
                            </form>
                        </div></div></div>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    
    <?php elseif (isset($_GET['view']) && $_GET['view'] === 'raporlar'): ?>
    <div class="top-nav">
        <div class="d-flex align-items-center gap-2">
            <div class="user-avatar"><?php echo mb_substr($_SESSION['adsoyad'], 0, 1); ?></div>
            <strong>Raporlar</strong>
        </div>
        <a href="<?php echo $_SERVER['PHP_SELF']; ?>" class="btn btn-outline-primary btn-sm"><i class="fas fa-arrow-left"></i> Geri</a>
    </div>
    
    <div class="card">
        <div class="card-header"><i class="fas fa-calendar-week"></i> Hafta Raporu</div>
        <div class="card-body">
            <form method="post" class="row g-3">
                <div class="col-md-9">
                    <select name="selected_week" class="form-select" required>
                        <option value="">Hafta seçin...</option>
                        <?php
                        $weeks = $db->prepare("SELECT * FROM haftalar WHERE user_id = ? ORDER BY hafta_baslangic DESC");
                        $weeks->execute([$_SESSION['user_id']]);
                        foreach ($weeks->fetchAll() as $w):
                        ?>
                        <option value="<?php echo $w['id']; ?>"><?php echo htmlspecialchars($w['hafta_araligi']); ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="col-md-3">
                    <button type="submit" name="generate_week_report" class="btn btn-primary w-100"><i class="fas fa-file-alt"></i> Oluştur</button>
                </div>
            </form>
        </div>
    </div>
    
    <div class="card">
        <div class="card-header"><i class="fas fa-calendar-alt"></i> Tarih Aralığı Raporu</div>
        <div class="card-body">
            <form method="post" class="row g-3">
                <div class="col-md-4"><label>Başlangıç</label><input type="date" name="baslangic_tarihi" class="form-control" required></div>
                <div class="col-md-4"><label>Bitiş</label><input type="date" name="bitis_tarihi" class="form-control" required></div>
                <div class="col-md-4 d-flex align-items-end"><button type="submit" name="generate_date_range_report" class="btn btn-primary w-100"><i class="fas fa-file-alt"></i> Oluştur</button></div>
            </form>
        </div>
    </div>
    
    <?php if (!empty($rapor_text)): ?>
    <div class="card">
        <div class="card-header bg-success"><i class="fas fa-check"></i> <?php echo htmlspecialchars($rapor_title); ?></div>
        <div class="card-body">
            <textarea id="raporText" class="form-control mb-3" rows="10" readonly><?php echo htmlspecialchars($rapor_text); ?></textarea>
            <button type="button" class="btn btn-success" onclick="copyReport()"><i class="fas fa-copy"></i> Kopyala</button>
        </div>
    </div>
    <?php endif; ?>
    
    <?php else: ?>
    <div class="top-nav">
        <div class="d-flex align-items-center gap-2">
            <div class="user-avatar"><?php echo mb_substr($_SESSION['adsoyad'], 0, 1); ?></div>
            <div>
                <strong><?php echo htmlspecialchars($_SESSION['adsoyad']); ?></strong>
                <div class="small text-muted"><?php echo $currentWeek ? htmlspecialchars($currentWeek['hafta_araligi']) : 'Hafta seçin'; ?></div>
            </div>
        </div>
        <div class="d-flex gap-2 flex-wrap">
            <a href="?view=raporlar" class="btn btn-outline-warning btn-sm"><i class="fas fa-file-alt"></i></a>
            <?php if (isAdmin()): ?><a href="?view=admin" class="btn btn-outline-danger btn-sm"><i class="fas fa-cog"></i></a><?php endif; ?>
            <button class="btn btn-outline-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#settingsModal"><i class="fas fa-user-cog"></i></button>
            <a href="?logout" class="btn btn-outline-danger btn-sm"><i class="fas fa-sign-out-alt"></i></a>
        </div>
    </div>
    
    <div class="steps">
        <div class="step <?php echo !$currentWeek ? 'active' : ''; ?>"><span>1</span> Hafta Seç</div>
        <div class="step <?php echo $currentWeek && empty($mesaiKayitlari) ? 'active' : ''; ?>"><span>2</span> Gün Seç</div>
        <div class="step <?php echo $currentWeek && !empty($mesaiKayitlari) ? 'active' : ''; ?>"><span>3</span> Veri Gir</div>
    </div>
    
    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span><i class="fas fa-calendar-week"></i> Hafta Seçimi</span>
            <button class="btn btn-light btn-sm" data-bs-toggle="collapse" data-bs-target="#newWeekForm"><i class="fas fa-plus"></i> Yeni</button>
        </div>
        <div class="card-body">
            <div class="collapse mb-3" id="newWeekForm">
                <div class="p-3 bg-light rounded">
                    <form method="post" class="row g-2 align-items-end">
                        <div class="col-8"><label>Başlangıç Tarihi</label><input type="date" name="hafta_baslangic" class="form-control" required></div>
                        <div class="col-4"><button type="submit" name="create_week" class="btn btn-success w-100"><i class="fas fa-check"></i></button></div>
                    </form>
                </div>
            </div>
            
            <div class="row g-2">
                <div class="col-md-3">
                    <form method="get">
                        <select name="year" class="form-select" onchange="this.form.submit()">
                            <?php foreach ($yearOptions as $y): ?>
                            <option value="<?php echo $y; ?>" <?php echo $selectedYear == $y ? 'selected' : ''; ?>><?php echo $y; ?></option>
                            <?php endforeach; ?>
                        </select>
                    </form>
                </div>
                <div class="col-md-9">
                    <?php if (!empty($allWeeks)): ?>
                    <div class="dropdown">
                        <button class="btn btn-primary dropdown-toggle w-100" data-bs-toggle="dropdown">
                            <?php echo $currentWeek ? '📅 ' . htmlspecialchars($currentWeek['hafta_araligi']) : 'Hafta seçin...'; ?>
                        </button>
                        <ul class="dropdown-menu w-100" style="max-height: 300px; overflow-y: auto;">
                            <?php foreach ($allWeeks as $w): ?>
                            <li><a class="dropdown-item <?php echo (isset($_SESSION['current_week']) && $_SESSION['current_week'] == $w['id']) ? 'active' : ''; ?>" href="?select_week=<?php echo $w['id']; ?>&year=<?php echo $selectedYear; ?>"><?php echo htmlspecialchars($w['hafta_araligi']); ?></a></li>
                            <?php endforeach; ?>
                        </ul>
                    </div>
                    <?php else: ?>
                    <p class="text-muted mb-0 py-2">Bu yıl için hafta yok. <a href="#" data-bs-toggle="collapse" data-bs-target="#newWeekForm">Oluştur</a></p>
                    <?php endif; ?>
                </div>
            </div>
        </div>
    </div>
    
    <?php if ($currentWeek): ?>
    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span><i class="fas fa-edit"></i> Mesai Girişi</span>
            <div class="dropdown">
                <button class="btn btn-light btn-sm dropdown-toggle" data-bs-toggle="dropdown"><i class="fas fa-cog"></i></button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><button class="dropdown-item" data-bs-toggle="modal" data-bs-target="#editWeekModal"><i class="fas fa-edit"></i> Düzenle</button></li>
                    <li><form method="post"><input type="hidden" name="week_id" value="<?php echo $currentWeek['id']; ?>"><button type="submit" name="delete_week" class="dropdown-item text-danger" onclick="return confirm('Silmek istediğinize emin misiniz?')"><i class="fas fa-trash"></i> Sil</button></form></li>
                </ul>
            </div>
        </div>
        <div class="card-body">
            <label class="form-label mb-2"><i class="fas fa-hand-pointer"></i> Gün seçin:</label>
            <div class="d-flex flex-wrap gap-2 mb-4">
                <?php
                $startDate = new DateTime($currentWeek['hafta_baslangic']);
                $filledDates = array_column($mesaiKayitlari, 'tarih');
                
                for ($i = 0; $i < 7; $i++):
                    $day = clone $startDate;
                    $day->modify("+$i days");
                    $dateStr = $day->format('Y-m-d');
                    $isFilled = in_array($dateStr, $filledDates);
                    $isWeekend = isHaftaSonu($dateStr);
                    
                    $classes = 'day-btn';
                    if ($isFilled) $classes .= ' filled';
                    if ($isWeekend) $classes .= ' weekend';
                ?>
                <div class="<?php echo $classes; ?>" data-date="<?php echo $dateStr; ?>">
                    <div class="day-name"><?php echo getGunAdi($dateStr); ?></div>
                    <div class="day-date"><?php echo formatTarih($dateStr); ?></div>
                    <?php if ($isFilled): ?><i class="fas fa-check text-success"></i><?php endif; ?>
                </div>
                <?php endfor; ?>
            </div>
            
            <form method="post" class="row g-3">
                <div class="col-md-3"><label>Tarih</label><input type="date" name="tarih" id="inputTarih" class="form-control" value="<?php echo $currentWeek['hafta_baslangic']; ?>" required></div>
                <div class="col-md-5"><label>Açıklama</label><input type="text" name="aciklama" id="inputAciklama" class="form-control" placeholder="Yapılan iş..." required></div>
                <div class="col-md-2"><label>Saat</label><input type="number" name="saat" step="0.5" min="0" max="24" class="form-control" placeholder="8"></div>
                <div class="col-md-2 d-flex align-items-end"><button type="submit" name="submit" class="btn btn-success w-100"><i class="fas fa-save"></i></button></div>
                <div class="col-12"><div class="form-check"><input type="checkbox" name="is_resmi_tatil" class="form-check-input" id="resmiTatil"><label class="form-check-label" for="resmiTatil">Resmi Tatil</label></div></div>
            </form>
        </div>
    </div>
    
    <?php if (!empty($mesaiKayitlari)): ?>
    <div class="card">
        <div class="card-header d-flex justify-content-between"><span><i class="fas fa-list"></i> Kayıtlar</span><button class="btn btn-light btn-sm" onclick="toggleEdit()"><i class="fas fa-edit"></i></button></div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table mb-0">
                    <thead><tr><th>Gün</th><th>Açıklama</th><th>Saat</th><th class="edit-col" style="display:none;">İşlem</th></tr></thead>
                    <tbody>
                        <?php foreach ($mesaiKayitlari as $m): ?>
                        <tr class="<?php echo $m['is_resmi_tatil'] ? 'row-tatil' : (isPazar($m['tarih']) ? 'row-pazar' : ''); ?>">
                            <td><strong><?php echo getGunAdi($m['tarih']); ?></strong> <span class="text-muted"><?php echo formatTarih($m['tarih']); ?></span></td>
                            <td><?php echo htmlspecialchars($m['aciklama']); ?> <?php if ($m['is_resmi_tatil']): ?><span class="badge bg-info">Tatil</span><?php endif; ?></td>
                            <td><?php echo (float)$m['saat'] > 0 ? '<strong>' . $m['saat'] . '</strong> saat' : '-'; ?></td>
                            <td class="edit-col" style="display:none;">
                                <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editRecord<?php echo $m['id']; ?>"><i class="fas fa-edit"></i></button>
                                <form method="post" style="display:inline;"><input type="hidden" name="record_id" value="<?php echo $m['id']; ?>"><button type="submit" name="delete_record" class="btn btn-sm btn-outline-danger" onclick="return confirm('Sil?')"><i class="fas fa-trash"></i></button></form>
                            </td>
                        </tr>
                        <div class="modal fade" id="editRecord<?php echo $m['id']; ?>"><div class="modal-dialog"><div class="modal-content">
                            <form method="post">
                                <div class="modal-header"><h5>Düzenle</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                                <div class="modal-body">
                                    <input type="hidden" name="id" value="<?php echo $m['id']; ?>">
                                    <div class="mb-3"><label>Tarih</label><input type="date" name="tarih" class="form-control" value="<?php echo $m['tarih']; ?>" required></div>
                                    <div class="mb-3"><label>Açıklama</label><input type="text" name="aciklama" class="form-control" value="<?php echo htmlspecialchars($m['aciklama']); ?>" required></div>
                                    <div class="mb-3"><label>Saat</label><input type="number" name="saat" step="0.5" class="form-control" value="<?php echo $m['saat']; ?>"></div>
                                    <div class="form-check"><input type="checkbox" name="is_resmi_tatil" class="form-check-input" <?php echo $m['is_resmi_tatil'] ? 'checked' : ''; ?>><label class="form-check-label">Resmi Tatil</label></div>
                                </div>
                                <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">İptal</button><button type="submit" name="edit" class="btn btn-primary">Kaydet</button></div>
                            </form>
                        </div></div></div>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
            
            <div class="row g-3 p-3 bg-light">
                <div class="col-4"><div class="summary-box"><div class="number"><?php echo $toplam_saat; ?></div><div class="label">Toplam Saat</div></div></div>
                <div class="col-4"><div class="summary-box"><div class="number"><?php echo $pazar_sayisi; ?></div><div class="label">Pazar</div></div></div>
                <div class="col-4"><div class="summary-box"><div class="number"><?php echo $resmi_tatil_sayisi; ?></div><div class="label">Tatil</div></div></div>
            </div>
        </div>
    </div>
    <?php endif; ?>
    
    <div class="modal fade" id="editWeekModal"><div class="modal-dialog"><div class="modal-content">
        <form method="post">
            <div class="modal-header"><h5>Hafta Düzenle</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body">
                <input type="hidden" name="week_id" value="<?php echo $currentWeek['id']; ?>">
                <div class="mb-3"><label>Başlangıç Tarihi</label><input type="date" name="hafta_baslangic" class="form-control" value="<?php echo $currentWeek['hafta_baslangic']; ?>" required></div>
            </div>
            <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">İptal</button><button type="submit" name="edit_week" class="btn btn-primary">Kaydet</button></div>
        </form>
    </div></div></div>
    <?php endif; ?>
    
    <div class="modal fade" id="settingsModal"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header"><h5><i class="fas fa-user-cog"></i> Ayarlar</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <form method="post">
                <h6>Şifre Değiştir</h6>
                <div class="mb-2"><input type="password" name="current_password" class="form-control" placeholder="Mevcut şifre" required></div>
                <div class="mb-2"><input type="password" name="new_password" class="form-control" placeholder="Yeni şifre" required></div>
                <div class="mb-3"><input type="password" name="confirm_new_password" class="form-control" placeholder="Yeni şifre tekrar" required></div>
                <button type="submit" name="change_password" class="btn btn-warning w-100"><i class="fas fa-key"></i> Değiştir</button>
            </form>
        </div>
    </div></div></div>
    
    <?php endif; ?>
</div>

<footer>© <?php echo date('Y'); ?> Mesai Takip</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
document.querySelectorAll('.day-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        document.getElementById('inputTarih').value = this.dataset.date;
        document.querySelectorAll('.day-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        document.getElementById('inputAciklama').focus();
    });
});

function toggleEdit() {
    document.querySelectorAll('.edit-col').forEach(col => {
        col.style.display = col.style.display === 'none' ? '' : 'none';
    });
}

document.getElementById('userSearch')?.addEventListener('input', function() {
    const filter = this.value.toLowerCase();
    document.querySelectorAll('#userTable tbody tr').forEach(row => {
        row.style.display = row.textContent.toLowerCase().includes(filter) ? '' : 'none';
    });
});

// MOBİL VE HTTP UYUMLU KOPYALAMA FONKSİYONU
function copyReport() {
    var copyText = document.getElementById("raporText");
    
    // Mobilde klavye açılmasını engellemek için readonly olduğundan emin olalım
    // ancak seçilebilir olması için focus gerekli
    copyText.focus();
    
    // Mobil uyumluluk için seçim
    copyText.select();
    copyText.setSelectionRange(0, 99999); // Mobil cihazlar için
    
    // 1. Yöntem: Modern Clipboard API (Sadece HTTPS veya Localhost'ta çalışır)
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(copyText.value)
            .then(() => {
                alert('Rapor başarıyla kopyalandı!');
            })
            .catch(err => {
                console.error('Clipboard API hatası:', err);
                fallbackCopy(copyText); // Hata verirse eski yönteme düş
            });
    } else {
        // 2. Yöntem: Güvenli olmayan (HTTP) bağlantı veya eski tarayıcılar
        fallbackCopy(copyText);
    }
}

function fallbackCopy(textArea) {
    try {
        var successful = document.execCommand('copy');
        if (successful) {
            alert('Rapor kopyalandı!');
        } else {
            alert('Kopyalama başarısız oldu. Lütfen metni manuel seçip kopyalayınız.');
        }
    } catch (err) {
        alert('Tarayıcınız kopyalamayı desteklemiyor. Lütfen manuel kopyalayınız.');
    }
}
</script>
</body>
</html>