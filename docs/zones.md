---
sidebar_position: 32
---

# Zones

Package `org.ciprix.zones`. Areas of the field defined in code, used to assist the driver, slow the
robot down or choose a behavior from where the robot is.

| Class | Shape |
| --- | --- |
| `Rectangle2d` | Axis-aligned rectangle from two opposite corners. |
| `Circle2d` | Center (fixed or supplied each cycle, for a moving target) and radius. |
| `Polygon2d` | Any polygon, clockwise or not, including self-intersecting ones (even-odd rule). |
| `Zone2d<Z>` | Pairs any of the above with an enum constant so you can ask "which zone am I in?". |

All shapes implement `ZoneIO`: `contains(point)`, `distanceTo(point)` and `getCenter()`.
Coordinates follow the WPILib field frame (origin at the Blue Alliance corner).

```java
Rectangle2d trench = new Rectangle2d(new Translation2d(4.0, 0.0), new Translation2d(5.5, 1.3));
Circle2d hub = new Circle2d(new Translation2d(4.6, 4.0), 1.2);

boolean inTrench = trench.contains(drive.getPose().getTranslation());
double toHub = hub.distanceTo(drive.getPose().getTranslation()); // meters
```

## Naming zones

```java
enum FieldZone { TRENCH, HUB }

List<Zone2d<FieldZone>> zones = List.of(
    new Zone2d<>(FieldZone.TRENCH, trench),
    new Zone2d<>(FieldZone.HUB, hub));

Optional<FieldZone> where = zones.stream()
    .filter(z -> z.contains(pose.getTranslation()))
    .map(Zone2d::getId)
    .findFirst();
```

## Alliance mirroring

Define zones once for the Blue Alliance and mirror them when you are on Red with
`AllianceFlipUtil.apply(polygon, flipX, flipY, useAllianceColor)`, the same way you mirror poses
with `AllianceFlipUtil.apply(pose)`. Pass `true` for the axes to mirror (usually both for a
rotationally symmetric field, or only X for a mirrored one) and `true` for `useAllianceColor` so
it only flips on Red.

## Using zones for driving

`ZoneSuctionModifier`, `ZoneRepulsionModifier` and `DualZoneSuctionBumpModifier` take `Polygon2d`
zones and change the driver's translation input. See [Driving helpers](driving-helpers.md).
