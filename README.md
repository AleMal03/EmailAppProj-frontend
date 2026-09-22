# MailApp - Frontend Client (JavaFX)

![Java](https://img.shields.io/badge/Java_22-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-FF0000?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![TCP/Sockets](https://img.shields.io/badge/TCP_Sockets-00599C?style=for-the-badge&logo=linux&logoColor=white)

## 🎓 Contesto
Progetto sviluppato per il corso di Programmazione III presso il Corso di Laurea in Informatica dell'Università degli Studi di Torino.
Questo repository contiene l'interfaccia utente (Client) di un sistema di posta elettronica, un'applicazione desktop reattiva progettata per interfacciarsi con un server dedicato tramite protocollo di rete custom.

> ⚠️ **Architettura Client-Server:** Questo repository contiene esclusivamente l'applicativo Client sviluppato in JavaFX. 
> Il Server associato a questo progetto è disponibile qui: [EmailApp backend](https://github.com/AleMal03/EmailAppProj-backend)

## 🏗 Architettura & Stack Tecnologico
- **Pattern MVC Reattivo:** Separazione netta tra logica di business (`DataModel`) e controller della UI. L'interfaccia sfrutta pesantemente il sistema di Data Binding di JavaFX (`Property` e `Bindings`) per aggiornarsi automaticamente al mutare dello stato.
- **Comunicazione di Rete (Socket TCP):** Integrazione client-server tramite Socket di rete diretti su TCP, con scambio di messaggi basato su un protocollo JSON customizzato (`ServerRequest` e `ServerResponse`). La conversione strutturata dei payload è gestita tramite la libreria Google Gson.
- **Multithreading & Concurrency:** Le operazioni di I/O (chiamate al server e I/O su file) sono demandate a ThreadPool in background per non bloccare l'interfaccia grafica e mantenere il sistema efficiente e scalabile. Gli aggiornamenti della UI vengono riassegnati al thread principale in totale sicurezza tramite appositi `Platform.runLater()`.
- **Polling & Caching Locale:** Un `ScheduledExecutorService` interroga il server ogni 5 secondi per sincronizzare la inbox. Le email vengono storicizzate localmente (`data/{user}.json`) garantendo resilienza, backup offline e ricaricamenti istantanei.
  >⚠️ **Nota:** La gestione della persistenza tramite file (invece di un DBMS relazionale) è stata espressamente richiesta dalle specifiche di progetto per valutare le competenze di gestione manuale dei lock concorrenti.
- **UI Frameworks:** Utilizzo di ControlsFX e Ikonli per arricchire l'interfaccia grafica con componenti avanzati e grafica vettoriale.

## ✨ Funzionalità
* **Gestione Sessione:** Login basato su verifica sintattica lato client ed esistenza dell'account lato server.
* **Operazioni Mailbox:** Lettura, invio, risposta (`Reply` e `ReplyAll`), inoltro (`Forward`) ed eliminazione dei messaggi.
* **Validazione Dinamica:** Verifica preventiva in real-time degli indirizzi email inseriti (sia sintattica che di esistenza) per notificare indirizzi inesistenti prima di processare l'invio.
* **Notifiche In-App:** Overlay non bloccante per messaggi informativi e di errore, temporizzati per scomparire automaticamente, uniti ad avvisi sonori all'arrivo di nuove email.

## 🚀 Installazione e Avvio
**Prerequisiti di Sistema**
*   **Java Development Kit (JDK):** Versione 22.
*   **Build Tool:** Maven.

**Setup dell'Ambiente**
1. Clonare il repository in locale e posizionarsi nella cartella radice:
   ```bash
   git clone https://github.com/AleMal03/EmailAppProj-frontend
   cd EmailAppProj-frontend
   ```
3. Avviare l'applicazione tramite il plugin JavaFX Maven preconfigurato:
   ```bash
   mvn clean javafx:run
   ```
Il client è configurato per cercare il server all'indirizzo IP 127.0.0.1 sulla porta TCP 5555. Assicurarsi che l'applicativo server sia in esecuzione localmente per testarne le funzionalità complete.
