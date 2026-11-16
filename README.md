<div align="center">

# Overview

**Launcher Android moderno e minimalista inspirado no GNOME 51 ("A Coruña") / libadwaita.**

Construído do zero em **Kotlin** e **Jetpack Compose**, com foco em gestos fluidos (60/120 Hz), personalização completa e fidelidade à estética limpa do GNOME para dispositivos móveis.

![Versão](https://img.shields.io/badge/vers%C3%A3o-1.0.0-3584e4?style=flat-square)
![Android](https://img.shields.io/badge/Android-12%2B%20(API%2031--35)-3ddc84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff?style=flat-square&logo=kotlin&logoColor=white)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-GPL--3.0-orange?style=flat-square)

[Recursos](#recursos) • [Capturas de tela](#capturas-de-tela) • [Compilar e instalar](#compilar-e-instalar) • [Arquitetura](#arquitetura) • [Roadmap](#roadmap)

</div>

---

## Sobre o projeto

O **Overview** traz para o Android a organização espacial e a clareza visual do **GNOME Shell** e da linguagem **libadwaita** — unindo uma tela inicial leve sobre o papel de parede do sistema, um **Dash** fixo para os favoritos e uma visão geral (**Overview**) com busca instantânea, pastas e grade paginada por gestos.

Código-fonte oficial: **[github.com/vimi-tt/Overview](https://github.com/vimi-tt/Overview)**

## Recursos

### Tela inicial e relógios
- **Grade personalizável (`3×3` a `6×6`)**: adicione atalhos diretamente da grade do Overview ou via seletor, posicione livremente na grade e redimensione widgets do sistema (`AppWidgetHost`).
- **6 estilos de relógio com pré-visualização ao vivo**:
  - **GNOME Clássico**: horário e data centralizados com dígitos tabulares.
  - **Empilhado**: hora e minuto em blocos verticais com cor de destaque.
  - **Analógico Adwaita**: mostrador vetorial minimalista desenhado em `Canvas`.
  - **Editorial lateral**: horário à esquerda, barra de destaque e data à direita.
  - **Cápsula compacta**: pílula discreta inspirada na barra superior do GNOME.
  - **Cápsula completa (Resumo)**: barra de canto a canto estilo Resumo com saudação dinâmica, data completa e horário.
- **Opacidade e contraste adaptativo**: ajuste de transparência (`0%` a `100%`) nos estilos com fundo, além de detecção automática de cores do papel de parede (`WallpaperManager.OnColorsChangedListener`).
- **Bloqueio de layout e rótulos**: oculte ou escale os nomes dos atalhos e impeça alterações acidentais na grade.

### Dash (Dock)
- De **3 a 6 aplicativos** fixados manualmente ou resolvidos automaticamente pelos padrões do sistema (discador, mensagens, navegador, câmera, configurações).
- Controle de **opacidade do fundo** e botão opcional de **barra de pesquisa rápida**.

### Overview, pastas e busca
- **Transições contínuas por gestos**: acompanhe o progresso do gesto vertical em tempo real com blur (`RenderEffect` + window blur no Android 12+), escurecimento do fundo, velocidade ajustável e estilos *GNOME Shell*, *Deslizar* ou *Esmaecer*.
- **Pastas sempre no início da grade**: crie e gerencie pastas pelas configurações ou direto pelo menu de contexto dos apps, com prévia **Grade 2×2** ou **Empilhada**, popup em **3 ou 4 colunas**, ordenação interna e fechamento automático ao iniciar um aplicativo.
- **Busca precisa em tempo real**: correspondência exata por prefixo de nome, palavras e segmentos CamelCase (ignorando acentos e maiúsculas/minúsculas), foco automático opcional, preservação/limpeza ao fechar e atalho de **pesquisa na web** (*DuckDuckGo*, *Google*, *Startpage* ou *Ecosia*).
- **Grade paginada**: colunas (`3` a `6`), escala de ícones, ordenação (A–Z, Z–A ou mais recentes) e gerenciamento de **aplicativos ocultos** (com opção de encontrá-los apenas pela busca).
- **Overview como aplicativo normal**: registrado com `CATEGORY_LAUNCHER`, aparece na grade como qualquer outro app e abre rapidamente suas configurações quando acionado de dentro do launcher.

### Gestos, aparência e acessibilidade
- **Gestos configuráveis**: personalize ações para **deslizar para cima**, **deslizar para baixo**, **toque duplo** e **botão Início na Home** (Overview, busca, notificações, configurações rápidas, configurações do Overview, papel de parede, assistente ou câmera), com 3 níveis de sensibilidade.
- **Aparência Adwaita**: tema Claro (`#fafafa`), Escuro (`#242424`) ou Automático, paleta de cores de destaque do GNOME, **pacotes de ícones** compatíveis com launchers, formato de ícone (*Círculo*, *Quadrado arredondado* ou *Quadrado*) e **pontos de notificação** via `NotificationListenerService`.
- **Acessibilidade**: respeito nativo à preferência global de **animações reduzidas** (`ANIMATOR_DURATION_SCALE`), dígitos tabulares e alvos de toque mínimos de 48 dp.

---

## Tecnologias e arquitetura

- **Linguagem & UI**: Kotlin + Jetpack Compose + Material 3 (design system próprio com tokens Adwaita)
- **Persistência**: Jetpack DataStore (`Preferences`) para preferências reativas por categoria + armazenamento JSON para workspace e pastas
- **Integração Android**: `LauncherApps` (eventos reativos de pacotes e animações de abertura), `AppWidgetHost`, `NotificationListenerService`, `WallpaperManager` e `Navigation Compose`

```text
app/src/main/java/io/github/vimitt/overview/
├── data/          # Repositórios (Apps, Settings DataStore, IconPacks, Badges, Widgets, HomeWorkspace, SystemPanelHelper)
├── di/            # AppContainer (injeção de dependências manual leve)
├── domain/        # Modelos imutáveis (AppInfo, HomeItem, LauncherPreferences) e casos de uso
└── ui/
    ├── theme/     # Tokens de cor Adwaita, raios, tipografia e utilitários de movimento reduzido
    ├── components/# AppIconItem, DashBar, HomeClock, OverviewSearchBar
    ├── home/      # HomeScreen, HomeViewModel, HomeUiState e seletores de apps/widgets
    ├── overview/  # OverviewScreen, OverviewViewModel, OverviewUiState (grade paginada, pastas e busca)
    ├── settings/  # SettingsNavHost, SettingsHubScreen, componentes libadwaita e 9 categorias de configuração
    └── LauncherRoot.kt
```

### Personalizar pacote base (opcional)
Caso deseje publicar um fork com outro identificador além de `io.github.vimitt.overview`:
1. Atualize `namespace` e `applicationId` em [`app/build.gradle.kts`](app/build.gradle.kts).
2. Renomeie os diretórios de pacote em `app/src/main/` e `app/src/test/` e ajuste as declarações `package` / `import`.

---

## Compilar e instalar

### Pré-requisitos
- **JDK 17+** e **Android SDK Platform 35** (`minSdk 31` / Android 12+, `targetSdk 35` / Android 15).
- **Android Studio** compatível com AGP 9.x ou linha de comando com `./gradlew`.
- Dispositivo físico ou emulador com Depuração USB ativada.

### Passo a passo

```bash
git clone https://github.com/vimi-tt/Overview.git
cd Overview
```

1. Caso não use a variável `ANDROID_HOME`, crie `local.properties` na raiz com `sdk.dir=/caminho/do/android-sdk`.
2. Compile e instale no aparelho conectado:

```bash
./gradlew installDebug
```

3. Execute os testes unitários e o lint:

```bash
./gradlew testDebugUnitTest lintDebug
```

4. Para gerar um APK release assinado localmente com sua própria keystore, copie `keystore.properties.example` para `keystore.properties` na raiz do repositório e preencha os dados da chave (ambos `keystore.properties` e `*.jks` ficam fora do Git via `.gitignore`):

```bash
./gradlew assembleRelease
```

### Definir o Overview como launcher padrão

- **Pelo Android**: acesse **Configurações → Apps → Apps padrão → App de início** e selecione **Overview** (ou pressione o botão Início e escolha **Overview → Sempre**).
- **Via `adb`**:

```bash
adb shell cmd role add-role-holder android.app.role.HOME io.github.vimitt.overview
```

Para restaurar o launcher padrão anterior via `adb`:

```bash
adb shell cmd role remove-role-holder android.app.role.HOME io.github.vimitt.overview
```

---

## Roadmap

- [x] Estrutura base Android 15 (`HOME` + `LAUNCHER` + `singleTask` + `onNewIntent`)
- [x] Tema Adwaita claro/escuro/automático + suporte a animações reduzidas
- [x] Tela inicial com papel de parede, 6 estilos de relógio com opacidade, grade configurável e widgets (`AppWidgetHost`)
- [x] Dash configurável (3–6 apps, opacidade e barra de busca)
- [x] Overview com blur `RenderEffect`, transições configuráveis, busca precisa por prefixo e atalho web
- [x] Pastas de aplicativos fixadas no início da grade e gerenciamento completo
- [x] Gestos configuráveis na Home e no botão Início com ajuste de sensibilidade
- [x] Central de Configurações completa no estilo GNOME / libadwaita (`0.1.0-beta-1`)
- [x] Pastas diretamente na tela inicial, painel próprio de configurações rápidas estilo GNOME e fonte Adwaita Sans embarcada
- [x] Internacionalização completa (inglês padrão + português), tela inicial multipáginas, App Shortcuts, `PinItemRequest`, desinstalação direta, ícone monocromático, Predictive Back e backup/restauração JSON (`1.0.0`)

---

## Créditos e licença

- Inspiração visual e de usabilidade no **[GNOME Shell / libadwaita](https://gitlab.gnome.org/GNOME/libadwaita)** (sem utilização de assets oficiais).
- Organização por categorias e subtítulos dinâmicos das configurações inspirada no **[Lawnchair](https://github.com/LawnchairLauncher/lawnchair)** (sem cópia de código ou recursos).
- Distribuído sob a licença **[GPL-3.0](LICENSE)**.
