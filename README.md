# Ciprix

[![CI](https://github.com/23Daniel3/Ciprix/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/23Daniel3/Ciprix/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/23Daniel3/Ciprix?display_name=tag)](https://github.com/23Daniel3/Ciprix/releases)

Biblioteca para robôs FRC em Java, baseada em **máquinas de estado e requests**. É instalada
como qualquer biblioteca de fornecedor da WPILib (vendordep) e traz o padrão de projeto pronto
para uma equipe nova: FSM, subsistemas com IO, motores de todos os fornecedores, simulação,
visão, energia e autoteste.

| Pasta | O que é |
| --- | --- |
| `core/`, `ctre/`, `phoenix5/`, `rev/`, `pathplanner/`, `processor/` | Os módulos da biblioteca, publicados como `org.ciprix:ciprix-<módulo>` |
| [`vendordep/`](./vendordep) | Os arquivos de vendordep (a versão é preenchida na publicação) |
| [`templates/skeleton/`](./templates/skeleton) | O menor robô completo no padrão, para copiar e começar um robô novo |
| [`docs/`](./docs) | Guias de arquitetura, simulação, visão, hardware, robô 3D e migração para 2027 |

## Instalar em um robô

No VS Code da WPILib, use **WPILib: Manage Vendor Libraries → Install new library (online)** e
cole as URLs dos módulos que o robô usa. O `Ciprix.json` é obrigatório; os outros dependem do
hardware.

| Vendordep | URL | Conteúdo |
| --- | --- | --- |
| `Ciprix.json` | `https://23daniel3.github.io/Ciprix/Ciprix.json` | FSM, `StateSubsystem`, abstrações de motor e encoder, simulação, visão, logging, utilitários |
| `Ciprix-CTRE.json` | `https://23daniel3.github.io/Ciprix/Ciprix-CTRE.json` | TalonFX, TalonFXS, CANcoder, diagnóstico do CAN (Phoenix 6) |
| `Ciprix-Phoenix5.json` | `https://23daniel3.github.io/Ciprix/Ciprix-Phoenix5.json` | TalonSRX e VictorSPX (Phoenix 5) |
| `Ciprix-REV.json` | `https://23daniel3.github.io/Ciprix/Ciprix-REV.json` | SPARK MAX, SPARK Flex e encoders REV |
| `Ciprix-PathPlanner.json` | `https://23daniel3.github.io/Ciprix/Ciprix-PathPlanner.json` | Integrações com PathPlanner e Choreo |

A Ciprix exige os vendordeps do **AdvantageKit** e do **WPILibNewCommands**, e cada módulo de
hardware exige o vendordep do fornecedor (Phoenix 6, Phoenix 5, REVLib, PathPlannerLib). O VS Code
avisa se faltar algum.

O processador de anotações (gera os `XxxCommands` de cada subsistema) entra no `build.gradle`:

```groovy
def ciprixJson = new groovy.json.JsonSlurper().parseText(new File(projectDir.getAbsolutePath() + "/vendordeps/Ciprix.json").text)
annotationProcessor "org.ciprix:ciprix-processor:$ciprixJson.version"
```

O jeito mais rápido de começar é copiar o [`templates/skeleton`](./templates/skeleton), que já
vem com tudo isso configurado.

## O padrão

1. O piloto fala com `SuperStructureCommands.manageRequests(...)`, que escreve um `RobotRequest`.
2. A máquina de estados geral da `SuperStructure` decide o estado do robô.
3. No `onEnter` de cada estado geral, a `SuperStructure` define o request de cada subsistema.
4. Cada subsistema é um `StateSubsystem<Request, State, Inputs, IO>` com a sua própria máquina
   de estados declarativa (`StateMachine` + `bindRequest(...)`), e só fala com o hardware pela
   camada IO.
5. O comando padrão de cada subsistema é o **relay** gerado por `@AutoCommandFactory`
   (`XxxCommands.defaultCommand(superStructure, xxx)`), que entrega o request da `SuperStructure`.
6. O copiloto é o backup manual: os comandos `XxxCommands.<request>(xxx)` sobrepõem o request
   de um subsistema enquanto o botão está pressionado.

Detalhes em [Arquitetura](./docs/arquitetura.md).

## O que a Ciprix traz

| Recurso | Guia |
| --- | --- |
| Máquinas de estado com transições temporizadas, estados de segurança e override do copiloto | [Arquitetura](./docs/arquitetura.md) |
| Motores que guardam a intenção do código, com un-jam e saúde (Talon FX/FXS, SPARK, SRX/SPX, PWM) | [Arquitetura](./docs/arquitetura.md#motores) |
| Simulação com física e bateria usando a mesma configuração do robô | [Simulação](./docs/simulacao.md) |
| Robô 3D no AdvantageScope com o CAD da equipe | [Robô 3D](./docs/robo-3d.md) |
| Visão por AprilTags pronta (Limelight), com filtros que dizem por que cada pose ficou de fora | [Visão](./docs/visao.md) |
| Alertas de hardware, gerenciamento de bateria, autoteste no pit e checklist da fila | [Hardware, energia e pit](./docs/hardware-energia-e-pit.md) |

## Desenvolver a biblioteca

```bash
# Formatação, build e testes de todos os módulos
./gradlew build

# Template consumindo a biblioteca pelo código-fonte (composite build)
cd templates/skeleton && ./gradlew build

# Repositório Maven e vendordeps locais (saem em build/repos/releases e build/vendordeps)
./gradlew publish generateVendordeps
```

Dentro deste repositório, o `settings.gradle` do template faz `includeBuild('../..')`, então o
Gradle troca os artefatos publicados pelo código-fonte local. Um projeto copiado para fora baixa
a versão publicada indicada nos vendordeps.

## Publicar uma versão

A versão fica em [`gradle.properties`](./gradle.properties) (`ciprixVersion`, no formato
`<temporada>.<minor>.<patch>`).

1. Atualize `ciprixVersion` e a versão em `templates/skeleton/vendordeps/Ciprix*.json` no mesmo PR.
2. Depois do merge na `main`, crie uma release com a tag `v<versão>` (por exemplo `v2026.2.0`).
3. O workflow [`publish.yml`](./.github/workflows/publish.yml) compila, testa e publica o
   repositório Maven em `https://23daniel3.github.io/Ciprix/repo` e os vendordeps em
   `https://23daniel3.github.io/Ciprix/`, na branch `gh-pages`.

O workflow recusa a publicação se a tag não bater com `ciprixVersion`.

## Licença

O código derivado dos templates da WPILib segue a [licença da WPILib](./WPILib-License.md).
