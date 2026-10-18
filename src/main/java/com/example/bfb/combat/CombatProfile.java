package com.example.bfb.combat;

/**
 * Параметры под конкретный античит.
 * Значения подобраны эмпирически из паблик-конфигов 2025-2026.
 */
public record CombatProfile(
        float rotationSpeed,
        float rotationSmooth,
        float rotationJitter,
        float gcdTolerance,
        int   attackDelayMin,
        int   attackDelayMax,
        float hitAngleThreshold,
        float hitboxExpand,
        boolean silentRotation,
        boolean onlyWhenMoving,
        boolean requireLineOfSight
) {
    public static CombatProfile forMode(CombatMode mode) {
        return switch (mode) {
            case GRIM -> new CombatProfile(
                    8f, 90f, 0f, 0f,
                    3, 5, 30f, 0f,
                    true, false, true
            );
            case VULCAN -> new CombatProfile(
                    15f, 140f, 0.05f, 0.0001f,
                    4, 7, 45f, 0.05f,
                    true, true, true
            );
            case MATRIX -> new CombatProfile(
                    12f, 120f, 0.08f, 0.0002f,
                    3, 6, 40f, 0f,
                    true, false, true
            );
            case POLAR -> new CombatProfile(
                    5f, 70f, 0.03f, 0f,
                    5, 9, 25f, 0f,
                    true, true, true
            );
            case REALLYWORLD -> new CombatProfile(
                    6f, 80f, 0.04f, 0f,
                    4, 7, 30f, 0f,
                    true, true, true
            );
            case SPOOKYTIME -> new CombatProfile(
                    10f, 100f, 0.06f, 0.0001f,
                    3, 5, 35f, 0f,
                    true, false, true
            );
            case DONUTSMP -> new CombatProfile(
                    7f, 85f, 0.02f, 0f,
                    4, 8, 28f, 0f,
                    true, true, true
            );
            case VIMEMC, RAIDMINE -> new CombatProfile(
                    15f, 130f, 0.10f, 0.0003f,
                    3, 5, 50f, 0.05f,
                    true, false, true
            );
            default -> new CombatProfile(
                    30f, 250f, 0.2f, 1f,
                    1, 2, 90f, 0.3f,
                    false, false, false
            );
        };
    }
}