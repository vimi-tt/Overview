# Overview

**Overview** é um launcher Android em código aberto inspirado na estética e nos fluxos táteis do **GNOME 51 ("A Coruña") / libadwaita**, adaptado para uso mobile com toques e gestos (referência: Phosh / GNOME Shell touch).

Construído do zero em **Kotlin** e **Jetpack Compose**, com foco em fluidez (60/120 Hz), simplicidade visual e respeito às preferências de tema e acessibilidade do sistema.

> **Licença**: [GPL-3.0](LICENSE)

---

## Capturas de tela

_Pendente: capturas de tela da Home, Dash e Overview em dispositivo físico com Android 15 serão adicionadas após a validação visual._

---

## Funcionalidades desta versão (MVP)

- **Tela inicial transparente**: exibe o papel de parede do sistema (`windowShowWallpaper`), relógio opcional e o **Dash** fixo na parte inferior.
- **Dash de favoritos**: até 5 aplicativos essenciais selecionados automaticamente quando disponíveis (telefone, mensagens, navegador, câmera e configurações).
- **Overview por gestos**: deslize para cima na tela inicial (ou toque no indicador acima do Dash) para abrir o Overview; deslize para baixo, pressione Voltar ou aperte Home para retornar.
- **Blur com `RenderEffect`**: desfoque configurável da camada inicial e do fundo da janela (Android 12+).
- **Busca em tempo real**: filtra aplicativos instantaneamente ao digitar, ignorando acentos e maiúsculas/minúsculas.
- **Grade paginada horizontalmente**: aplicativos ordenados alfabeticamente com rolagem por páginas e indicadores de página.
- **Atualização reativa**: escuta eventos de instalação, remoção, atualização e suspensão via `LauncherApps.Callback`.
- **Tema Adwaita & Acessibilidade**: suporte a tema claro (`#fafafa`), escuro (`#242424`), automático (sistema), cor de destaque configurável (padrão azul Adwaita `#3584e4`) e respeito à configuração global de **animações reduzidas** (`ANIMATOR_DURATION_SCALE`).

---

## Arquitetura

```text
app/src/main/java/io/github/vimitt/overview/
├── data/          # LauncherAppsRepository, AppIconCache (LruCache), PreferencesRepository (DataStore)
├── di/            # AppContainer (injeção de dependências manual leve)
├── domain/        # Modelos (AppInfo, LauncherPreferences) e casos de uso
└── ui/
    ├── theme/     # Tokens de cor Adwaita, formas, tipografia e utilitários de movimento reduzido
    ├── components/# AppIconItem, DashBar, OverviewSearchBar
    ├── home/      # HomeScreen, HomeViewModel, HomeUiState
    ├── overview/  # OverviewScreen, OverviewViewModel, OverviewUiState
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
3. Conecte o celular via USB, selecione a variante `debug` e clique em **Run** (ou instale via terminal abaixo, já que a Activity principal responde apenas a `HOME` para não duplicar ícone no drawer de outros launchers).

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
- [ ] **Painel superior / Configurações rápidas** (barra de status integrada, toggles rápidos no estilo GNOME)
- [ ] **Pastas / Grupos de aplicativos** no Overview com edição por toque longo
- [ ] **Tela de personalização** (escolha de cor de destaque, intensidade do blur, alternar relógio, fixar/reordenar favoritos do Dash)
- [ ] **Gestos adicionais e refinamentos de blur** (progresso contínuo no gesto de arrastar para abrir/fechar, suporte à fonte Adwaita Sans embarcada)
