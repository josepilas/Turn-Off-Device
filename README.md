# TurnOffDevice-TV

**A tiny Android app that turns off a rooted device with a single tap. Designed for Android TV boxes, but also usable on phones and tablets.**

## English

### Overview

TurnOffDevice-TV is a small, purpose-built Android utility that acts as a virtual power button. Its goal is simple: **tap the launcher icon and shut the device down**.

The project was created for rooted Android TV boxes where shutting down the device would otherwise require navigating the system UI or using a remote-control app. Instead of opening a full interface, the app performs the shutdown flow immediately when launched.

### How it works

The app uses a transparent `Activity` and does not present a traditional user interface. When launched, it:

1. Checks whether usable root access is available.
2. Executes shutdown commands through `su`.
3. Tries a small list of fallback commands to improve compatibility across Android environments.
4. Finishes as soon as the shutdown command is successfully started.

The application does **not** need network access or a background service.

### Root requirement

The current version is intended for **rooted devices**. A normal third-party Android application cannot freely perform a device shutdown operation, so this project uses root access to execute the required system commands.

Support for **Shizuku** may be added in a future version to provide an alternative execution path on compatible non-rooted devices.

### Android TV support

Android TV is a first-class target of the project. The APK declares Android TV launcher support and includes a **16:9 TV banner**, allowing the app to appear directly in the TV launcher alongside other applications.

A square icon is also included for mobile and other standard launcher environments.

### Design goals

- One tap to shut down.
- No traditional app UI.
- Very small and lightweight APK.
- No network communication.
- No accounts or user data collection.
- No unnecessary permissions.
- Simple Java implementation using the Android framework.
- Compatibility-oriented root command fallbacks.

### Project structure

The project intentionally keeps the implementation small. The main behavior is handled by a single Android `Activity`, with launcher resources and Android TV assets alongside the standard Gradle/Android project files.

### Building

Open the project in Android Studio or build it with the Gradle wrapper included in the repository.

A rooted Android device or Android TV box is required for the current shutdown implementation.

### Security and privacy

TurnOffDevice-TV is deliberately narrow in scope. The application exists to perform one privileged operation: shutting down the device.

It does not require Internet access, does not collect analytics, and does not include a remote service or account system. The root commands used by the app are predefined by the application rather than entered by the user.

> **Important:** Root access itself is highly privileged. Only install and use this application on devices you control and trust.

---

## Português

### Visão geral

O TurnOffDevice-TV é um utilitário Android pequeno e específico que funciona como um **botão de desligar virtual**. A proposta é simples: **clicar no ícone do launcher e desligar o dispositivo**.

O projeto foi criado para TV Boxes Android com root, nas quais desligar o aparelho poderia exigir navegar pela interface do sistema ou usar um aplicativo de controle remoto. Em vez de abrir uma interface completa, o aplicativo executa o fluxo de desligamento imediatamente ao ser iniciado.

### Como funciona

O aplicativo usa uma `Activity` transparente e não apresenta uma interface tradicional. Ao ser iniciado, ele:

1. Verifica se existe acesso root utilizável.
2. Executa os comandos de desligamento através do `su`.
3. Tenta alguns comandos alternativos para melhorar a compatibilidade entre diferentes ambientes Android.
4. Encerra a execução assim que um comando de desligamento é iniciado com sucesso.

O aplicativo **não precisa de acesso à rede nem de serviço em segundo plano**.

### Requisito de root

A versão atual é destinada a **dispositivos com root**. Um aplicativo Android de terceiros comum não pode simplesmente realizar o desligamento do aparelho, então este projeto utiliza o acesso root para executar os comandos de sistema necessários.

O suporte ao **Shizuku** poderá ser adicionado futuramente como uma alternativa em dispositivos compatíveis sem root.

### Suporte a Android TV

Android TV é um dos alvos principais do projeto. O APK declara suporte ao launcher de Android TV e possui um **banner 16:9**, permitindo que o aplicativo apareça diretamente na tela inicial da TV junto aos demais aplicativos.

Também existe um ícone quadrado para celulares e outros launchers convencionais.

### Objetivos do projeto

- Um toque para desligar.
- Sem interface tradicional.
- APK pequeno e leve.
- Sem comunicação com a Internet.
- Sem contas ou coleta de dados do usuário.
- Sem permissões desnecessárias.
- Implementação simples em Java usando o framework do Android.
- Fallbacks de comandos root para melhorar a compatibilidade.

### Estrutura do projeto

A implementação foi mantida propositalmente pequena. O comportamento principal fica concentrado em uma única `Activity` Android, acompanhado pelos recursos do launcher e pelos assets específicos do Android TV, além dos arquivos padrão do projeto Gradle/Android.

### Compilação

Abra o projeto no Android Studio ou compile-o usando o Gradle Wrapper incluído no repositório.

Para a implementação atual de desligamento, é necessário utilizar um dispositivo Android ou TV Box com root.

### Segurança e privacidade

O TurnOffDevice-TV foi projetado para ter um escopo extremamente restrito. O aplicativo existe para realizar uma única operação privilegiada: desligar o dispositivo.

Ele não precisa de acesso à Internet, não coleta analytics e não possui servidor remoto nem sistema de contas. Os comandos root utilizados pelo aplicativo são predefinidos no próprio programa, e não fornecidos pelo usuário.

> **Importante:** acesso root é uma capacidade altamente privilegiada. Instale e utilize este aplicativo apenas em dispositivos que você controla e nos quais confia.
