# 💬 UfuMessenger - Aplicativo de Mensagens Instantâneas

> Trabalho final desenvolvido para a disciplina de Programação para Dispositivos Móveis da Universidade Federal de Uberlândia (UFU).

## 👥 Desenvolvedores
* Isac Silva
* Marcel Teixeira Chiarelo
* William Menezes Damascena
* Gilete Leite Iasbeck

---

## 🚀 Sobre o Projeto

O **UfuMessenger** é um aplicativo de mensagens instantâneas completo para Android, focado na sincronização de dados em tempo real, segurança e integração fluida com o hardware do smartphone. O projeto utiliza uma arquitetura moderna e reativa, garantindo uma experiência de usuário rica e segura.

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Kotlin
* **Interface (UI):** Jetpack Compose (Material 3)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Injeção de Dependências:** Dagger Hilt
* **Banco de Dados (Real-time):** Firebase Firestore
* **Autenticação:** Firebase Auth (Telefone/OTP - *Passwordless*)
* **Armazenamento de Mídia (Bucket):** Supabase Storage
* **Carregamento de Imagens:** Coil (com cache em disco automático)

---

## 🏆 Requisitos Atendidos (O que o app faz)

Conforme os requisitos do trabalho (valor: 48 pontos), o UfuMessenger implementa:

1.  **Sincronização em Tempo Real:** Mensagens chegam instantaneamente usando `addSnapshotListener` do Firestore. **(Req 3, 10)**
2.  **Segurança Avançada:** Todas as mensagens de texto são **criptografadas ponta a ponta (AES)** antes de serem salvas no banco de dados e descriptografadas apenas na tela do chat. Um banner visual no chat informa sobre a proteção. **(Req 11)**
3.  **Sistema de Presença (Status):** Exibe status **Online/Offline** na barra superior do chat, sincronizado via ciclo de vida do Android na `MainActivity`. **(Req 8)**
4.  **Notificações Push (Hack Local):** O app escuta novas mensagens em segundo plano via Firestore na `MainActivity` e dispara notificações locais nativas do Android quando o app está minimizado, simulando o comportamento de Push real. **(Req 4)**
5.  **Integração de Sensores (GPS, Câmera e Microfone) - (12 Pontos):** O app utiliza os sensores do dispositivo de forma integrada ao chat. **(Req 15)**
    * **GPS (Localização):** Permite enviar a localização atual como um ponto no mapa. **(Req 15)**
    * **Câmera:** Permite tirar uma foto diretamente pelo app e enviá-la como mídia. **(Req 15, 7)**
    * **Microfone:** Permite gravar e enviar mensagens de voz curtas. **(Req 15, 7)**
6.  **Uso Offline:** O Firestore e o Coil gerenciam o cache local, permitindo ler mensagens antigas e enviar novas mensagens que são sincronizadas ao recuperar a conexão. **(Req 10)**
7.  **Funcionalidades de Chat:**
    * Divisor de data reativo (Hoje, Ontem, etc.) agrupado no `LazyColumn`. **(Req 9)**
    * Pesquisa de mensagens por palavra-chave direto na `ChatScreen`. **(Req 14)**
    * Confirmações de leitura (ticks azuis e cinzas). **(Req 3)**
    * Criação e gerenciamento de grupos. **(Req 6)**
    * Fixação de mensagens importantes no topo do chat. **(Req 13)**
    * Autenticação via número de telefone (OTP) com login persistente. **(Req 1, 12)**
    * Sincronização com a agenda de contatos local do aparelho. **(Req 5)**

---

## 💻 Como Rodar o Projeto

1.  Clone este repositório.
2.  Abra o projeto no **Android Studio (versão Hedgehog+)**.
3.  Certifique-se de que o arquivo `google-services.json` (do Firebase) está na pasta `app/`.
4.  Certifique-se de que as chaves da API do Supabase estão configuradas corretamente no código.
5.  Execute o app em um emulador ou dispositivo físico.

## 📄 Licença

Este projeto é licenciado sob a licença MIT - consulte o arquivo [LICENSE.md](LICENSE.md) para detalhes.
