# Visão

A visão tem dois níveis: o pipeline pronto da biblioteca, que funciona para qualquer equipe, e as
regras extras que cada equipe pode somar no próprio robô.

## Nível 1: `AprilTagVision` (biblioteca)

Funciona com Limelight sem precisar do `LimelightHelpers`:

```java
vision =
    new AprilTagVision(
            drivetrain::addVisionMeasurement, drivetrain::getPose, () -> drivetrain.getState().Speeds)
        .withCamera("Front", new VisionIOLimelight("limelight-front", drivetrain::getRotation))
        .withCamera("Back", new VisionIOLimelight("limelight-back", drivetrain::getRotation), 1.5)
        .withFilter(
            new VisionFilter()
                .field(AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltWelded))
                .maxDistance(4.0));
```

- `VisionIOLimelight` lê toda imagem nova da fila do NetworkTables, então nenhuma se perde e
  nenhuma é usada duas vezes, e entrega as poses MegaTag 1 e MegaTag 2.
- `VisionFilter` descarta, com motivo: pose fora do campo, robô fora do chão ou inclinado, tags
  longe demais, tag única ambígua (MegaTag 1), giro rápido demais para o MegaTag 2 e, se ligado
  com `maxJump(metros)`, o "salto" de uma tag única para longe da estimativa enquanto ela é
  confiável. Depois de 1 s sem nenhuma pose aceita a estimativa deixa de ser confiável e o robô se
  reencontra.
- `VisionStdDevs` calcula o desvio padrão por distância² dividido pelo número de tags. No MegaTag
  2 o ângulo é o do próprio giroscópio, então o desvio do ângulo é ignorado.
- O número no `withCamera` (1.5 acima) multiplica os desvios de uma câmera menos confiável.

No log, cada câmera tem `Vision/<câmera>/AcceptedPoses`, `RejectedPoses`, `LastRejection`,
`AcceptedCount`, `RejectedCount` e `TagPoses`, e uma câmera que para de responder aparece nos
alertas de hardware. No replay, use `new VisionIO() {}` em cada câmera.

## Nível 2: regras do seu robô

Quando a equipe precisar de um filtro mais rígido, mantenha um subsistema `Vision` no projeto do
robô com os mapas da equipe (distância, velocidade, ambiguidade) e some as regras da biblioteca.
Regras que costumam valer a pena:

- **Cada imagem julgada uma vez.** Se ler a Limelight como "último valor" a cada ciclo, a mesma
  imagem entra no estimador em vários ciclos seguidos e o estimador confia demais nela.
- **Limites do campo da temporada**, robô no chão, giro rápido e salto de tag única
  (`maxJump(metros)`).
- **Ângulo só do MegaTag 1 com 2 ou mais tags.** Nos outros casos use o ângulo do giroscópio e
  ignore o desvio do ângulo: corrigir o giroscópio com ele mesmo só injeta o atraso da câmera.
- Grave o motivo de cada rejeição no log (por exemplo `Vision/<CÂMERA>/RejectReason`).

Para afinar: olhe os motivos de rejeição e as poses no AdvantageScope depois de um treino. Se uma
regra descarta imagens boas demais, afrouxe a constante dela.

## Campo

Use o layout do campo do evento: `AprilTagFields.k2026RebuiltAndymark` ou
`AprilTagFields.k2026RebuiltWelded`. Os limites do filtro vêm desse layout.
