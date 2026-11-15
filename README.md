# Overview

**Overview** é um launcher Android em código aberto inspirado na estética e nos fluxos táteis do **GNOME 51 ("A Coruña") / libadwaita**, adaptado para uso mobile com toques e gestos (referência: Phosh / GNOME Shell touch).

Construído do zero em **Kotlin** e **Jetpack Compose**, com foco em fluidez (60/120 Hz), simplicidade visual e respeito às preferências de tema e acessibilidade do sistema.

> **Licença**: [GPL-3.0](LICENSE)

---

## Capturas de tela

_Pendente: capturas de tela da Home, Dash e Overview em dispositivo físico com Android 15 serão adicionadas após a validação visual._

---

## Funcionalidades atuais

- **Tela inicial transparente**: exibe o papel de parede do sistema (`windowShowWallpaper`), relógio opcional, atalhos/widgets redimensionáveis na grade e o **Dash** fixo na parte inferior.
- **Dash de favoritos**: de 3 a 6 aplicativos selecionados automaticamente ou fixados pelo usuário, com opacidade configurável e barra de busca rápida opcional.
- **Overview por gestos**: deslize para cima na tela inicial (ou toque no indicador/busca do Dash) para abrir o Overview com acompanhamento contínuo do gesto; deslize para baixo, pressione Voltar ou aperte Home para retornar. Arraste apps direto da grade para a tela inicial.
- **Desfoque e animações do Overview**: `RenderEffect` e blur de janela (Android 12+), escurecimento do fundo configurável, estilo de transição (*GNOME Shell*, *Deslizar* ou *Esmaecer*), velocidade de animação, efeito de profundidade na tela inicial e animação de abertura dos aplicativos.
- **Busca em tempo real**: filtra aplicativos instantaneamente ao digitar, ignorando acentos e maiúsculas/minúsculas, com foco automático opcional, busca por nome do pacote, preservação/limpeza ao fechar e atalho de pesquisa na web (DuckDuckGo, Google, Startpage ou Ecosia).
- **Pastas no Overview**: criação, edição e exclusão de pastas de aplicativos com visualização em grade `2 × 2` ou empilhada, popup configurável em 3 ou 4 colunas, ordenação interna e opção de fechar ao abrir um app.
- **Gestos personalizáveis**: ações configuráveis na tela inicial para deslizar para cima, deslizar para baixo, toque duplo e botão Início (Overview, busca, notificações, configurações rápidas, configurações do Overview, papel de parede, assistente ou câmera), além de ajuste de sensibilidade.
- **Grade paginada horizontalmente**: colunas, escala de ícones, ordenação (A–Z, Z–A ou mais recentes) e gerenciamento de apps ocultos.
- **Configurações no estilo GNOME/libadwaita**: central organizada por categorias (**Aparência**, **Tela inicial**, **Dock**, **Grade de apps**, **Overview**, **Busca**, **Pastas**, **Gestos** e **Sobre**) com pacotes de ícones compatíveis com launchers, formato de ícone, cores de destaque do GNOME e pontos de notificação (`NotificationListenerService`).
- **Atualização reativa**: escuta eventos de instalação, remoção, atualização e suspensão via `LauncherApps.Callback`.
- **Tema Adwaita & Acessibilidade**: suporte a tema claro (`#fafafa`), escuro (`#242424`), automático (sistema), cor de destaque configurável (padrão azul Adwaita `#3584e4`) e respeito à configuração global de **animações reduzidas** (`ANIMATOR_DURATION_SCALE`).

---

## Arquitetura

```text
app/src/main/java/io/github/vimitt/overview/
├── data/          # LauncherAppsRepository, SettingsRepository (DataStore), IconPackRepository, NotificationBadgeRepository, HomeWorkspaceRepository
├── di/            # AppContainer (injeção de dependências manual leve)
├── domain/        # Modelos (AppInfo, LauncherPreferences) e casos de uso
└── ui/
    ├── theme/     # Tokens de cor Adwaita, formas, tipografia e utilitários de movimento reduzido
    ├── components/# AppIconItem, DashBar, OverviewSearchBar
    ├── home/      # HomeScreen, HomeViewModel, HomeUiState, seletores de apps/widgets
    ├── overview/  # OverviewScreen, OverviewViewModel, OverviewUiState
    ├── settings/  # SettingsNavHost, SettingsHubScreen, componentes libadwaita e subtelas por categoria
    └── LauncherRoot.kt
```

### Alterar o usuário do pacote base

Atualmente o projeto usa o pacote `io.github.vimitt.overview`. Caso queira trocar `vimitt` por outro identificador:

1. Atualize `namespace` e `applicationId` em [`app/build.gradle.kts`](app/build.gradle.kts).
2. Renomeie a pasta `vimitt` em `app/src/main/` e `app/src/test/` e ajuste as declarações `package` / `import` nos arquivos `.kt`.

---

## Como compilar e instalar

### Pré-requisitos

- **Android Studio** (versão recente compatível com AGP 9.x) ou **JDK 17+** e **Android SDK Platform 35**.
- Dispositivo físico com Android 12+ (testado visando **Android 15**, `minSdk 31`, `targetSdk 35`) com Depuração USB ativada.

### Via Android Studio

1. Clone o repositório e abra a pasta `Overview` no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Conecte o celular via USB, selecione a variante `debug` e clique em **Run** (ou instale via terminal abaixo). O **Overview** também é registrado com `CATEGORY_LAUNCHER` para aparecer como um aplicativo normal na grade e na tela inicial.

### Via linha de comando (`./gradlew` + `adb`)

1. Certifique-se de que `ANDROID_HOME` esteja definido (ou configure `sdk.dir=/caminho/do/android-sdk` em `local.properties`).
2. Compile e instale no dispositivo conectado:

```bash
./gradlew installDebug
```

3. Verifique se o dispositivo está conectado e abra o Overview diretamente via `adb`:

```bash
adb devices
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

4. Para executar os testes unitários:

```bash
./gradlew testDebugUnitTest
```

---

## Como definir o Overview como launcher padrão

### Pelo sistema (Android 15)

1. Pressione o botão **Home** (ou faça o gesto de início) e escolha **Overview** -> **Sempre**, **ou**
2. Acesse **Configurações → Apps → Apps padrão → App de início** e selecione **Overview**.

### Via `adb` (opcional)

```bash
adb shell cmd role add-role-holder android.app.role.HOME io.github.vimitt.overview
```

Para restaurar o launcher anterior do aparelho caso deseje voltar:

```bash
adb shell cmd role remove-role-holder android.app.role.HOME io.github.vimitt.overview
```

---

## Roadmap

- [x] Estrutura base Android 15 (`HOME` + `singleTask` + `onNewIntent`)
- [x] Tema Adwaita claro/escuro/automático + modo de animações reduzidas
- [x] Home transparente com papel de parede, relógio opcional e Dash inicial
- [x] Overview com busca em tempo real, `RenderEffect` blur e grade paginada reativa
- [x] **Tela inicial e widgets**: atalhos e widgets na tela inicial (`AppWidgetHost`, arrastar e soltar do Overview e reposicionamento livre na grade configurável)
- [x] **Configurações por categorias**: central com Aparência (pacote/forma de ícones, cores GNOME, pontos de notificação), Tela inicial, Dock, Grade de apps (apps ocultos/ordenação), Overview e Sobre
- [x] **Overview (desfoque e animações)**: intensidade do blur, opacidade do fundo, estilo de transição, velocidade, efeito de profundidade e progresso contínuo no gesto vertical
- [x] **Busca**: foco automático ao abrir, correspondência pelo pacote, limpeza ao fechar e atalho de pesquisa na web configurável
- [x] **Pastas**: criação e edição nas configurações e direto no Overview, pré-visualização em grade 2 × 2 ou empilhada e janela popup com 3 ou 4 colunas
- [x] **Gestos**: ações personalizáveis para deslizar para cima/baixo, toque duplo e botão Início na Home, com ajuste de sensibilidade
- [ ] **Refinamentos futuros**: pastas na tela inicial, painel de configurações rápidas próprio estilo GNOME e fonte Adwaita Sans embarcada
