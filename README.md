# ensiasd-monitor

Application Java 17 / Maven qui surveille automatiquement la page officielle ENSIASD:

https://ensiasd.uiz.ac.ma/

Le programme telecharge la page avec Jsoup, extrait uniquement les articles de la section `Actualites & Evenements`, calcule un hash SHA-256 du contenu normalise `titre|url`, compare la liste avec le dernier etat JSON, puis envoie des notifications uniquement lorsqu'un nouvel article apparait.

## Prerequis

- Java 17
- Maven 3.8+
- Un bot Telegram
- Les variables d'environnement suivantes:
  - `TELEGRAM_TOKEN`
  - `TELEGRAM_CHAT_ID`
  - `SMTP_HOST` (optionnel)
  - `SMTP_PORT` (optionnel)
  - `SMTP_USERNAME` (optionnel)
  - `SMTP_PASSWORD` (optionnel)
  - `EMAIL_FROM` (optionnel)
  - `EMAIL_TO` (optionnel)

## Execution locale

```bash
mvn clean package
mvn exec:java
```

Par defaut, l'etat JSON est stocke dans:

```text
src/main/resources/state.txt
```

Vous pouvez choisir un autre fichier avec:

```bash
STATE_FILE=/chemin/vers/state.txt mvn exec:java
```

Sous Windows PowerShell:

```powershell
$env:TELEGRAM_TOKEN="votre-token"
$env:TELEGRAM_CHAT_ID="votre-chat-id"
mvn exec:java
```

Configuration email optionnelle sous Windows PowerShell:

```powershell
$env:SMTP_HOST="smtp.example.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="votre-utilisateur"
$env:SMTP_PASSWORD="votre-mot-de-passe"
$env:EMAIL_FROM="bot@example.com"
$env:EMAIL_TO="destinataire@example.com"
mvn exec:java
```

La premiere execution initialise le fichier d'etat sans envoyer de notification. Les executions suivantes notifient seulement si une nouvelle URL d'article apparait dans la section surveillee.

## GitHub Actions

Le workflow `.github/workflows/monitor.yml`:

- s'execute toutes les 5 minutes avec cron;
- compile le projet Maven;
- execute `ma.imgharn.ensiasd.Main`;
- persiste le nouvel etat JSON dans `src/main/resources/state.txt` en le commitant dans le depot si necessaire.

Ajoutez ces secrets dans votre depot GitHub:

- `TELEGRAM_TOKEN`
- `TELEGRAM_CHAT_ID`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `EMAIL_FROM`
- `EMAIL_TO`

Chemin GitHub:

```text
Settings > Secrets and variables > Actions > New repository secret
```

Note: GitHub Actions peut parfois retarder les workflows planifies selon la charge de GitHub, mais la configuration utilise bien la frequence minimale de 5 minutes.
