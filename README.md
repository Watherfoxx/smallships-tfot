# Small Ships (TFOT) — Compilation avec IntelliJ IDEA

Ce dépôt est un projet **Architectury** qui génère deux variantes du mod : **Fabric** et **Forge**. Les modules principaux sont :

- `common` : code partagé
- `fabric` : implémentation Fabric
- `forge` : implémentation Forge

Les instructions ci‑dessous expliquent comment **compiler** le mod avec **IntelliJ IDEA**.

## Prérequis

- **IntelliJ IDEA** (Community ou Ultimate)
- **JDK 17** (requis par le `build.gradle`)
- Accès Internet pour que Gradle télécharge les dépendances

> Conseil : utilisez un JDK 17 installé localement, puis configurez IntelliJ pour l’utiliser à la fois pour le projet **et** pour Gradle.

## 1) Importer le projet dans IntelliJ

1. Ouvrez IntelliJ IDEA.
2. **File → Open…** puis sélectionnez le dossier racine `smallships-tfot`.
3. IntelliJ détecte un projet **Gradle**. Choisissez **Open as Project**.
4. Une fois le projet importé, attendez la **synchronisation Gradle**.

## 2) Configurer le JDK 17 dans IntelliJ

1. **File → Project Structure… → Project**
2. Définissez **Project SDK** sur **JDK 17**.
3. Vérifiez que **Project language level** est au moins `17`.

## 3) Configurer le JDK Gradle

1. **File → Settings… → Build, Execution, Deployment → Build Tools → Gradle**
2. Dans **Gradle JVM**, sélectionnez **JDK 17**.
3. Appliquez puis fermez.

> Cette étape est cruciale : Gradle compile avec Java 17 (`options.release = 17`).

## 4) Synchroniser Gradle et générer les sources

Si besoin, cliquez sur l’icône **Refresh** dans l’onglet **Gradle** (à droite) pour forcer la synchronisation.

## 5) Compiler le mod (Fabric et Forge)

Dans l’onglet **Gradle** d’IntelliJ :

- **Fabric** : `smallships → fabric → Tasks → build → build`
- **Forge** : `smallships → forge → Tasks → build → build`

Ou via le terminal intégré d’IntelliJ :

```bash
./gradlew :fabric:build
./gradlew :forge:build
```

Les JARs générés se trouvent dans :

- `fabric/build/libs/`
- `forge/build/libs/`

Les fichiers **sans** le suffixe `-dev` sont les builds « remappés » (distribuables).

## 6) Lancer en environnement de développement (optionnel)

Toujours via l’onglet **Gradle** :

- **Fabric** : `fabric → Tasks → loom → runClient`
- **Forge** : `forge → Tasks → loom → runClient`

Ces tâches téléchargent Minecraft et lancent un client de test avec le mod.

## 7) Nettoyer les builds (optionnel)

```bash
./gradlew clean
```

## Problèmes courants

- **Erreurs de version Java** : vérifiez que **Project SDK** et **Gradle JVM** utilisent **JDK 17**.
- **Dépendances non téléchargées** : relancez une synchronisation Gradle ou assurez‑vous que l’accès Internet est disponible.

---

Bon build ! 🚢
