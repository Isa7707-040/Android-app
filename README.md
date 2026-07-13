# JESKO Savdo — Android (Sotuvchi ilovasi)

Do'kon savdo tizimining mobil ilovasi. Sotuvchilar uchun mo'ljallangan: tezkor
mahsulot qidirish, savatga qo'shish (blok/dona), buyurtmani kassirga yuborish.
Dizayn `StoreSystem.Desktop` (WPF) ilovasi bilan bir xil — to'q ko'k fon va
oltin (gold) urg'u, **JESKO** brendi.

## Texnologiyalar

- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** (ViewModel + StateFlow)
- **Retrofit** + **Gson** (REST API)
- **DataStore** (sessiya va server manzilini saqlash)
- minSdk 24, targetSdk 34

## Loyihani ochish

1. Android Studio (Hedgehog yoki undan yangi) bilan ushbu papkani oching.
2. Android Studio Gradle wrapper'ni avtomatik tiklaydi va sinxronlaydi.
   - Agar `./gradlew` topilmasa: Android Studio sizdan Gradle versiyasini
     so'raydi yoki terminalda `gradle wrapper --gradle-version 8.9` ni ishga
     tushiring (`gradle-wrapper.jar` ni tiklash uchun).
3. `Run` tugmasini bosing.

## Server manzilini sozlash (MUHIM)

Ilova `StoreSystem.Api` server bilan ishlaydi. Ulanish muammosini hal qilish
uchun server manzili **ilova ichida** sozlanadi:

- Login oynasining yuqori o'ng burchagidagi **⚙ (sozlama)** tugmasini bosing.
- Server manzilini kiriting:
  - **Emulyator** uchun: `https://10.0.2.2:7134/`
  - **Real telefon** uchun: kompyuteringizning lokal IP manzili, masalan
    `https://192.168.1.50:7134/` (telefon va kompyuter bir Wi-Fi tarmog'ida
    bo'lishi shart).
- Manzil DataStore'da saqlanadi — keyingi safar qayta kiritish shart emas.

> Eslatma: server lokal o'z-o'zini imzolagan (self-signed) HTTPS sertifikatdan
> foydalangani uchun ilova development rejimida barcha sertifikatlarga ishonadi
> va cleartext (HTTP) trafikka ruxsat beradi. Productionda buni cheklang.

## Asosiy imkoniyatlar





- **Login**: server avtomatik sotuvchilar ro'yxatini ko'rsatadi → sotuvchini
  tanlaysiz → parol → "Meni eslab qol" belgilansa, keyingi safar to'g'ridan-
  to'g'ri kiradi (login oyna chiqmaydi).
- **Mahsulotlar**: tezkor qidiruv (masalan `pep` → barcha "Pepsi"), mahsulot
  kartasiga bosib **Blok / Dona** tanlash, **+/-** bilan son kiritish, savatga
  qo'shish.
- **Savat**: pozitsiyalarni tahrirlash (son o'zgartirish, o'chirish), umumiy
  summa, mijoz tanlash/qo'shish (yoki "Naqd xaridor") va **Kassirga yuborish**.
- **Sotuvlarim**: sotuvchining yuborgan/sotilgan buyurtmalari va statistikasi.
- **Profil**: sotuvchi ma'lumotlari, statistika, server manzili, chiqish.

## API bilan bog'liqlik (StoreSystem.Api)

| Funksiya | Endpoint |
|---|---|
| Sotuvchilar ro'yxati | `GET /api/Users/sellers` |
| Kirish | `POST /api/Users/login` |
| Mahsulotlar | `GET /api/Products` |
| Mijozlar | `GET /api/Clients`, `POST /api/Clients` |
| Buyurtma yaratish | `POST /api/Orders` |
| Tarix | `GET /api/Orders/history`, `GET /api/Orders/pending` |

Buyurtmadagi `quantity` doimo **dona** hisobida yuboriladi (ombor donada
hisoblanadi). Blok sotuvda: `dona = blok × quantityInBlock`, narx esa blok
narxidan bir donaga hisoblanadi.





