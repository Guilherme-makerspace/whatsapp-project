# 💬 Chat em Tempo Real — Android + Node.js

Projeto desenvolvido como tarefa guiada pelo professor **Leonardo Garcia Gomes**.

O objetivo é criar um aplicativo de chat em tempo real, onde dispositivos na mesma rede se comunicam através de um servidor Node.js usando **Socket.io**.

---

## 🗂 Estrutura do projeto

```
chat-server/
└── server.js          # Servidor Node.js

app/ (Android)
├── ChatViewModel.kt   # Lógica de conexão e mensagens
├── MainActivity.kt    # Interface (telas e componentes)
└── Screen.kt          # (removível) rotas de navegação
```

---

## ⚙️ Como funciona

1. O servidor Node.js fica "escutando" conexões na porta `3000`
2. O app Android conecta ao servidor via Socket.io informando o IP da máquina
3. Quando alguém envia uma mensagem, o servidor repassa para **todos os conectados**
4. O app diferencia mensagens próprias (direita) das dos outros (esquerda) pelo nome do autor

---

## 🖥 Configurando o servidor

### Pré-requisitos
- [Node.js](https://nodejs.org) instalado

### Comandos

```bash
# 1. Criar e entrar na pasta do servidor
mkdir chat-server && cd chat-server

# 2. Iniciar o projeto Node
npm init -y

# 3. Instalar o Socket.io
npm install socket.io

# 4. Rodar o servidor
node server.js
```

Servidor rodando em: `http://localhost:3000`

---

## 📱 Configurando o app Android

### Dependências (`build.gradle.kts`)

```kotlin
implementation("io.socket:socket.io-client:2.1.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.navigation:navigation-compose:2.8.7")
```

### Permissões (`AndroidManifest.xml`)

```xml
<!-- Acima de <application> -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Dentro de <application> -->
<application android:usesCleartextTraffic="true" ...>
```

---

## 🌐 Descobrindo o IP da máquina

Para conectar o celular ao servidor, você precisa do IP local da máquina:

```bash
# Windows
ipconfig
# Procure "Endereço IPv4" — ex: 192.168.0.105

# Mac / Linux
ifconfig
```

> Se estiver usando o **emulador Android**, use o IP `10.0.2.2` no lugar do IP local.

---

## 📋 Fluxo de dados

```
[Você digita] → ChatViewModel.sendMessage()
             → socket.emit("send_message")
             → Servidor Node.js
             → io.emit("receive_message") para todos
             → ChatViewModel recebe e adiciona à lista
             → Compose atualiza a tela automaticamente
```
