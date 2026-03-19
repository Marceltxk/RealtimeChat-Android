# ChatSphere: Aplicação de Chat em Tempo Real para Android

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-7F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.0-4285F4?style=for-the-badge&logo=jetpackcompose)
![Firebase](https://img.shields.io/badge/Firebase-B52A32?style=for-the-badge&logo=firebase)

ChatSphere é uma aplicação de chat moderna e funcional para Android, construída do zero com as tecnologias mais recentes do ecossistema Kotlin e Firebase. A aplicação oferece funcionalidades de mensagens em tempo real, tanto para conversas individuais como em grupo, com um foco numa arquitetura limpa e escalável.

## Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Arquitetura](#-arquitetura)
- [Configuração do Projeto](#-configuração-do-projeto)
  - [Pré-requisitos](#pré-requisitos)
  - [Configuração do Firebase (Backend)](#configuração-do-firebase-backend)
  - [Configuração do Cliente (Android)](#configuração-do-cliente-android)
  - [Configuração das Cloud Functions](#configuração-das-cloud-functions)
- [Liberar o acesso App Check](#configuração-do-app-check)

## 📖 Sobre o Projeto

O objetivo deste projeto foi desenvolver uma aplicação de chat completa, implementando as funcionalidades essenciais encontradas em aplicações de mensagens populares como WhatsApp e Telegram. O desenvolvimento foi focado na utilização de boas práticas, como a separação de responsabilidades (MVVM), injeção de dependência e programação reativa com Kotlin Flows.

## ✨ Funcionalidades

A aplicação implementa um conjunto robusto de funcionalidades:

#### 👤 **Autenticação e Utilizadores**
- [x] Registo e Login através de número de telemóvel (Firebase Phone Auth).
- [x] Validação de utilizadores existentes no login.
- [x] Sistema de Contactos (adicionar/remover utilizadores).
- [x] Pesquisa de utilizadores por nome.

#### 💬 **Mensagens e Conversas**
- [x] Envio de imagens e outros tipos de multimédia nas conversas.
- [x] Upload e visualização de fotos de perfil (Firebase Storage).
- [x] Conversas individuais e em grupo em tempo real.
- [x] Criação de novos grupos com seleção de múltiplos membros.
- [x] Identificação visual do remetente em grupos (nome e avatar).
- [x] Indicador de presença ("Online", "A escrever...", "Visto por último").
- [x] **Status de Mensagens:**
    - [x] ✓ Enviado (Sent)
    - [x] ✓✓ Entregue (Delivered)
    - [x] ✓✓ Lido (Read) - com ícone azul
- [x] **Notificações Push** via FCM para novas mensagens (individuais e em grupo).
- [x] Fixar/Desafixar mensagens numa conversa.
- [x] Pesquisa de mensagens dentro de uma conversa.
- [x] Apagar conversas da sua lista (soft delete, "apagar para mim").


## 🛠️ Tecnologias Utilizadas

O projeto é construído sobre uma stack moderna, separando claramente o frontend do backend.

### **Frontend (Aplicação Android)**
- **Linguagem:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) para uma UI declarativa e moderna.
- **Arquitetura:** MVVM (Model-View-ViewModel) com princípios de Clean Architecture.
- **Assincronismo:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) e [Flow](https://kotlinlang.org/docs/flow.html) para gestão de estado reativa e operações assíncronas.
- **Injeção de Dependência:** [Hilt](https://dagger.dev/hilt/) para gerir dependências de forma simples e robusta.
- **Navegação:** [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) para navegar entre os ecrãs.
- **Carregamento de Imagens:** [Coil](https://coil-kt.github.io/coil/) para carregar imagens de perfil de forma eficiente.

### **Backend (Firebase - BaaS)**
- **Autenticação:** [Firebase Authentication](https://firebase.google.com/docs/auth) (provedor de Telefone).
- **Base de Dados:** [Cloud Firestore](https://firebase.google.com/docs/firestore) como base de dados NoSQL em tempo real.
- **Lógica de Servidor:** [Cloud Functions for Firebase (v2)](https://firebase.google.com/docs/functions) escritas em **TypeScript** para automatizar tarefas de backend, como o envio de notificações.
- **Notificações Push:** [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging) para notificar os utilizadores de novas mensagens.
- **Armazenamento:** [Firebase Storage](https://firebase.google.com/docs/storage) (planeado para fotos de perfil e multimédia).

## 🏗️ Arquitetura

A aplicação segue o padrão de arquitetura **MVVM**.

- **View (Compose UI):** Ecrãs reativos que observam o estado do ViewModel e enviam eventos.
- **ViewModel:** Contém a lógica de UI e o estado do ecrã (`UiState`). Comunica com o repositório.
- **Repository:** Camada de abstração que lida com as fontes de dados (Firestore). É a única parte da aplicação que sabe de onde vêm os dados.
- **Model:** Classes de dados (`data class`) que representam as entidades da aplicação (User, Conversation, Message).

Esta separação garante que o código é testável, fácil de manter e escalar.

## 🚀 Configuração do Projeto

Para executar este projeto localmente, siga estes passos:

### Pré-requisitos
- Android Studio (versão mais recente recomendada).
- Conta Firebase.
- Node.js e npm (para as Cloud Functions).

### Configuração do App Check

Para proteger a sua aplicação contra abusos, o Firebase App Check bloqueia pedidos de clientes não verificados, mesmo em ambiente de desenvolvimento. Para que possa executar e testar a aplicação a partir do Android Studio num emulador ou dispositivo físico, precisa de registar manualmente um "token de depuração" para cada instalação.

O processo é simples e precisa de ser feito uma vez por cada dispositivo/emulador de teste.

**Passo 1: Executar a Aplicação e Ativar o App Check**

1.  Ligue um dispositivo físico com a depuração USB ativada ou inicie um Emulador Android.
2.  Compile e execute a aplicação no modo **debug** a partir do Android Studio.
3.  A primeira vez que a aplicação arrancar, ela irá tentar comunicar com o Firestore ou outro serviço Firebase. O App Check irá falhar (isto é esperado) e irá gerar um token de depuração nos logs da aplicação.

**Passo 2: Encontrar o Token de Depuração no Logcat**

1.  Com a aplicação em execução, abra a janela do **Logcat** no Android Studio (`View -> Tool Windows -> Logcat`).
2.  Na barra de pesquisa do Logcat, digite exatamente: `AppCheck`.
3.  Você verá uma linha de log (geralmente de cor azul, nível Debug) que se parece com isto:

    ```log
    D/com.google.firebase.appcheck: Enter this debug token in the Firebase console:
    [xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx]
    ```

4.  **Copie** o token alfanumérico longo que aparece (por exemplo, `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).

**Passo 3: Registar o Token de Depuração no Firebase Console**

1.  Abra o seu projeto no **[Firebase Console](https://console.firebase.google.com/)**.
2.  No menu de Compilação à esquerda, vá para a secção **App Check**.
3.  Clique na aba **Apps** e, em seguida, clique no nome da sua aplicação Android.
4.  Irá abrir-se um painel à direita. Clique no menu de três pontos (⋮) e selecione **"Gerir tokens de depuração"** (Manage debug tokens).
5.  Clique em **"Adicionar token de depuração"**.
6.  **Cole** o token que copiou do Logcat no campo que aparece.
7.  Clique em **Salvar**.

**Passo 4: Reiniciar a Aplicação**

1.  Volte ao Android Studio e **execute a aplicação novamente** (ou feche-a e reabra-a) no mesmo dispositivo/emulador.
2.  Neste segundo arranque, a aplicação irá usar o token de depuração. O Firebase irá reconhecê-lo como um dispositivo de teste registado e todas as chamadas à base de dados e outros serviços irão funcionar como esperado.

*Este processo garante que apenas os dispositivos de desenvolvimento que aprovados podem aceder ao backend Firebase enquanto estão no modo de depuração.*

### Configuração do Firebase (Backend)
1. Crie um novo projeto no [Firebase Console](https://console.firebase.google.com/).
2. Adicione uma aplicação Android ao seu projeto Firebase. Siga as instruções para descarregar o ficheiro `google-services.json`.
3. Ative os seguintes serviços no seu projeto:
   - **Authentication:** Ative o provedor "Telefone".
   - **Firestore Database:** Crie uma base de dados no modo de produção.
   - **Storage:** Crie um bucket de armazenamento.
4. **Índices do Firestore:** Durante a utilização, a aplicação pode indicar no Logcat a necessidade de criar índices compostos. Basta seguir os links fornecidos pelo erro para os criar automaticamente.

### Configuração do Cliente (Android)
1. Clone este repositório.
2. Coloque o ficheiro `google-services.json` que descarregou na pasta `app/`.
3. Abra o projeto no Android Studio, aguarde a sincronização do Gradle e execute a aplicação.

### Configuração das Cloud Functions
1. Instale a Firebase CLI: `npm install -g firebase-tools`.
2. Faça login: `firebase login`.
3. Navegue até à pasta `functions` no terminal.
4. Instale as dependências: `npm install`.
5. Faça o deploy das funções: `firebase deploy --only functions`.

## 🔧 Serviços Google Cloud & Firebase Consumidos

A aplicação utiliza uma vasta gama de APIs do Google Cloud e Firebase para garantir funcionalidade, escalabilidade e segurança. Abaixo está um detalhe dos principais serviços consumidos e o seu papel no projeto:

### 🚀 Base da Aplicação (Core Services)

* **Cloud Firestore API:** O coração da aplicação. Utilizado como a base de dados NoSQL em tempo real para armazenar todos os dados, como utilizadores, conversas e mensagens.
* **Identity Toolkit API:** O backend do **Firebase Authentication**. Gere todo o ciclo de vida da autenticação do utilizador, incluindo a verificação de número de telemóvel, criação de contas e gestão de sessões.
* **Cloud Functions API:** Permite a execução de código de backend sem servidor. É usada para a lógica de envio de notificações push sempre que uma nova mensagem é criada.
* **Firebase Cloud Messaging API:** Responsável por entregar as notificações push para os dispositivos Android, alertando os utilizadores sobre novas mensagens em tempo real.
* **Cloud Storage for Firebase API:** Utilizado para o armazenamento de ficheiros binários, como as fotos de perfil dos utilizadores.

---
### ⚙️ Infraestrutura e Operações (Backend)

* **Eventarc API:** O sistema de "gatilhos" para as Cloud Functions v2. Ele deteta eventos (como uma nova escrita no Firestore) e invoca a função correspondente.
* **Cloud Pub/Sub API:** O sistema de mensagens que o Eventarc usa para notificar as Cloud Functions de forma assíncrona e fiável sobre os eventos que ocorreram.
* **Artifact Registry API:** Armazena as "imagens de container" das suas Cloud Functions. Cada deploy cria um novo pacote da sua função, que é guardado aqui.
* **Cloud Build API:** O serviço que automaticamente "constrói" a sua Cloud Function num container sempre que você faz o deploy, preparando-a para ser executada.
* **Cloud Logging API:** Centraliza todos os logs gerados pela Cloud Function, sendo uma ferramenta essencial para a depuração e monitorização do backend.
* **Cloud Runtime Configuration API:** Usada para gerir configurações dinâmicas para os serviços em execução na nuvem.

---
### 🛡️ Segurança e Gestão (Security & Management)

* **Identity and Access Management (IAM) API:** Gere as permissões, definindo quem (utilizadores, serviços) tem acesso a quais recursos no seu projeto Google Cloud.
* **Firebase App Check API:** Protege os seus serviços de backend, garantindo que os pedidos vêm apenas da sua aplicação autêntica e não de clientes não autorizados.
* **Token Service API:** Lida com a criação e validação de tokens de segurança usados entre os serviços Google.
* **Firebase Installations API:** Gere as instalações da sua aplicação para garantir que as notificações FCM sejam encaminhadas corretamente para os dispositivos certos.

---
### 🧠 Ferramentas de IA (AI Tools)

* **Gemini for Google Cloud API:** Utilizado como assistente de IA durante o ciclo de desenvolvimento para gerar código, explicar conceitos.