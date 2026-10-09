# Glyphora 📖

**Glyphora** est une application Android native moderne de lecture de documents et de livres électroniques, développée en **Kotlin**, **Jetpack Compose** et **Material 3**.

---

## 🌟 Caractéristiques clés

* **4 Formats supportés** :
  * 📘 **EPUB** : Rendu fidèle des chapitres XHTML, des styles CSS, des images et de la structure du livre.
  * 📄 **PDF** : Rendu vectoriel natif haute fidélité (`PdfRenderer`), navigation, zoom tactile et mémorisation de page.
  * 🌐 **HTML** : Nettoyage sécurisé via Jsoup, support des documents et articles locaux.
  * 📝 **TXT** : Lecture fluide, détection automatique d'encodage (UTF-8, UTF-16, ISO) et pagination virtuelle.
* **100% Hors-ligne & Respect de la vie privée** :
  * Aucune permission `INTERNET` déclarée dans le manifeste.
  * Aucun traceur, aucune publicité, aucun compte requis.
* **Ergonomie & Material 3** :
  * Thèmes Clair, Sombre, Système et Sépia pour le confort visuel.
  * Personnalisation typographique (taille, police Serif/Sans/Monospace, interligne, marges).
  * Storage Access Framework (SAF) respectant Scoped Storage.
* **Compilation gratuite dans le Cloud** :
  * Workflow GitHub Actions inclus pour générer automatiquement l'APK debug sans solliciter les ressources locales.

---

## 🛠️ Stack technique

* **Langage** : Kotlin 2.0.21
* **UI Toolkit** : Jetpack Compose (BOM 2024.10.00) & Material 3
* **Build System** : Gradle 8.9 & Android Gradle Plugin 8.5.2
* **Persistance** : AndroidX DataStore Preferences
* **Compatibilité** : Android 8.0+ (minSdk 26, targetSdk 35)

---

## 🚀 Compilation de l'APK via GitHub Actions

Une fois le projet poussé sur un dépôt GitHub :
1. Chaque `push` sur la branche `main` déclenche automatiquement le workflow `.github/workflows/build-apk.yml`.
2. Le job tourne sur les serveurs gratuits de GitHub (`ubuntu-latest`).
3. L'APK généré (`app-debug.apk`) est disponible en téléchargement direct dans l'onglet **Actions** > **Artifacts**.
