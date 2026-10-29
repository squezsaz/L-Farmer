# L-Farmer 🌾

Modern, modüler, Folia ve çoklu platform (Paper, Purpur, Spigot, Folia) destekli gelişmiş Minecraft Çiftçi eklentisi.

- **Yazar:** squezsaz
- **Sürüm Desteği:** 1.21.x - 26.x (26.3 dahil)
- **Java Sürümü:** Java 21+

---

## ✨ Özellikler

1. **Çoklu Platform & Folia Desteği:**
   - Standart Spigot / Paper / Purpur sunucularında standart zamanlayıcıları kullanır.
   - Folia sunucularında otomatik olarak `RegionScheduler`, `GlobalRegionScheduler` ve `AsyncScheduler` bölgesel iş parçacıklarına geçiş yapar.

2. **Veritabanı Desteği (HikariCP):**
   - SQLite (Yerel dosya)
   - MySQL / MariaDB (Yüksek performanslı bağlantı havuzu)

3. **Modüler Çiftçi Fonksiyonları:**
   - **Otomatik Tarım Hasadı:** Olgunlaşan buğday, havuç, patates, pancar, karpuz, balkabağı, şeker kamışı, kaktüs, bambu, nether wart, kakao ve tatlı orman meyvelerini toplar.
   - **Otomatik Tekrar Ekme:** Hasat edilen ekinlerin tabanına anında tohum/filiz eker.
   - **Mob & Spawner Toplama:** Çiftçi menzilindeki ölen mobların ve spawnerların droplarını anında çeker.
   - **Otomatik Satış:** Depolanan ürünleri yapılandırılan aralıklarla otomatik satarak parayı doğrudan sahibin hesabına aktarır.

4. **Yükseltmeler (Upgrades):**
   - **Depolama Kapasitesi:** Seviye 1'den 5'e (5.000 - 150.000+ ürün).
   - **Kapsama Alanı (Menzil):** 1 Chunk -> 3x3 Chunk -> 5x5 Chunk.
   - **Hasat Hızı:** 100 tick -> 60 tick -> 20 tick.
   - **Çifte Hasat (Şans):** %25, %50, %100 servet/çift drop şansı.
   - **Vergi İndirimi:** %20 kesintiden %0 kesintiye kadar indirim.

5. **Yakıt ve Maaş Sistemi:**
   - Kömür, lav kovası vb. eşyalarla tank doldurma veya para maaşı ödeyerek çalıştırma (Config'den açılıp kapatılabilir).

6. **Dahili Display Entity / Paket Tabanlı Hologram:**
   - Harici hiçbir eklentiye ihtiyaç duymadan, modern `TextDisplay` teknolojisiyle çiftçi üzerinde canlı istatistikler ve durum gösterir.

7. **Harici Eklenti Entegrasyonları:**
   - **Ada & Arazi Koruma:** SuperiorSkyblock2, BentoBox (BSkyBlock), Lands, GriefPrevention, WorldGuard, Towny, PlotSquared.
   - **Ekonomi & Puan:** Vault, PlayerPoints.
   - **Market & Fiyat:** ShopGUIPlus, EconomyShopGUI, `prices.yml`.
   - **Özel Model:** ItemsAdder mob / mobilya desteği.
   - **PlaceholderAPI (PAPI):** Özel placeholder değişkenleri.

8. **Çoklu Dil Desteği:**
   - Türkçe (`messages_tr.yml`)
   - İngilizce (`messages_en.yml`)

9. **Canlı Geçişli (Gradient) Renk Paleti:**
   - Hasat & Altın Kehribar teması (`<gradient:#ffb347:#ffcc33>` & `<gradient:#ff7e5f:#feb47b>`).

---

## 📌 Komutlar ve Yetkiler

| Komut | Açıklama | Yetki |
|---|---|---|
| `/farmer` (veya `/ciftci`, `/çiftçi`, `/lfarmer`) | Bulunulan chunk'taki çiftçiyi açar | `farmer.use` |
| `/farmer help` | Tüm komutları ve açıklamalarını gösteren yardım menüsü | `farmer.use` |
| `/farmer list` | Sahip olunan çiftçilerin listesini gösterir | `farmer.use` |
| `/farmer stats` | Oyuncunun toplam çiftçi istatistiklerini görüntüler | `farmer.use` |
| `/farmer remove` | Bulunulan chunk'taki çiftçiyi kaldırır ve eşyayı iade eder | `farmer.use` / `farmer.admin` |
| `/farmer admin [sayfa]` | Tüm sunucu çiftçilerini gösteren, ışınlanma ve yönetim sağlayan yönetici paneli | `farmer.admin` |
| `/farmer give <oyuncu> [adet]` | Oyuncuya çiftçi yerleştirme eşyası verir | `farmer.admin.give` |
| `/farmer reload` | Yapılandırma ve dil dosyalarını yeniler | `farmer.admin.reload` |

Limit Yetkileri: `farmer.limit.<sayı>` (Örn: `farmer.limit.5`) veya sınırsız için `farmer.limit.admin`.

---

## 🏷️ PlaceholderAPI Değişkenleri

- `%lfarmer_placed%` - Oyuncunun koyduğu aktif çiftçi sayısı
- `%lfarmer_max%` - Oyuncunun koyabileceği maksimum çiftçi limiti
- `%lfarmer_storage_total%` - Oyuncunun çiftçilerindeki toplam depolanmış ürün miktarı
- `%lfarmer_total_earned%` - Oyuncunun çiftçilerinden kazandığı toplam para
- `%lfarmer_total_collected%` - Oyuncunun çiftçilerinin topladığı toplam ürün sayısı
- `%lfarmer_chunk_has_farmer%` - Bulunulan chunkta çiftçi var mı (`true`/`false`)
- `%lfarmer_chunk_farmer_level%` - Bulunulan chunk'taki çiftçinin kapasite seviyesi
- `%lfarmer_chunk_farmer_owner%` - Bulunulan chunk'taki çiftçinin sahibi

---

## 🛠️ Derleme (Build)

Projeyi derlemek için:
```bash
mvn clean package
```
Oluşturulan `.jar` dosyası `target/L-Farmer-1.0.0.jar` konumundadır.
