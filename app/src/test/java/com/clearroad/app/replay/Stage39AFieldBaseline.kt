package com.clearroad.app.replay

/**
 * Frozen Stage 38.3 field baseline — 47 deduplicated CALM route sets.
 * Source: field logcat 2026-06-18 PM, 2026-06-19 AM/PM.
 */
internal object Stage39AFieldBaseline {

    private fun r(
        durationMin: Int,
        durationInTrafficMin: Int,
        distanceKm: Double,
        critical: Int,
    ): FieldRouteReplayRoute =
        FieldRouteReplayRoute(
            durationMin = durationMin,
            durationInTrafficMin = durationInTrafficMin,
            distanceKm = distanceKm,
            criticalManeuversCount = critical,
        )

    private fun set(
        id: String,
        bucket: FieldBucket,
        winner: Int,
        routes: List<FieldRouteReplayRoute>,
    ): FieldRouteSet =
        FieldRouteSet(
            id = id,
            bucket = bucket,
            routes = routes,
            fieldCalmWinnerIndex = winner,
        )

    val all: List<FieldRouteSet> =
        listOf(
            // 2026-06-18 PM (14)
            set("2026-06-18T21:23:54", FieldBucket.PREV_PM, 0, listOf(r(35, 35, 27.54, 16), r(38, 38, 21.53, 7), r(38, 38, 25.68, 15))),
            set("2026-06-18T21:24:06", FieldBucket.PREV_PM, 1, listOf(r(33, 33, 31.94, 16), r(33, 33, 28.60, 14), r(37, 37, 28.19, 11))),
            set("2026-06-18T21:24:32", FieldBucket.PREV_PM, 1, listOf(r(41, 41, 32.44, 10), r(39, 39, 32.27, 16), r(47, 47, 46.63, 20))),
            set("2026-06-18T21:24:47", FieldBucket.PREV_PM, 0, listOf(r(112, 112, 171.26, 9), r(114, 114, 176.28, 12), r(120, 120, 181.19, 16))),
            set("2026-06-18T21:24:59", FieldBucket.PREV_PM, 1, listOf(r(101, 101, 150.61, 13), r(102, 102, 163.62, 17), r(110, 110, 167.57, 24))),
            set("2026-06-18T21:25:12", FieldBucket.PREV_PM, 0, listOf(r(17, 17, 14.62, 7), r(18, 18, 14.02, 10))),
            set("2026-06-18T21:25:30", FieldBucket.PREV_PM, 2, listOf(r(17, 17, 10.39, 12), r(16, 16, 13.45, 11), r(19, 19, 11.98, 11))),
            set("2026-06-18T21:25:51", FieldBucket.PREV_PM, 1, listOf(r(76, 76, 110.76, 12), r(80, 80, 115.30, 20), r(82, 82, 115.48, 19))),
            set("2026-06-18T21:26:07", FieldBucket.PREV_PM, 1, listOf(r(14, 14, 10.87, 7), r(14, 14, 11.66, 5), r(16, 16, 13.00, 7))),
            set("2026-06-18T21:26:25", FieldBucket.PREV_PM, 0, listOf(r(17, 17, 11.99, 5), r(18, 18, 14.75, 10), r(20, 20, 12.00, 8))),
            set("2026-06-18T21:26:42", FieldBucket.PREV_PM, 0, listOf(r(11, 11, 4.45, 4), r(12, 12, 4.64, 5))),
            set("2026-06-18T21:27:01", FieldBucket.PREV_PM, 0, listOf(r(106, 106, 157.67, 13), r(108, 108, 161.21, 11), r(108, 108, 162.69, 16))),
            set("2026-06-18T21:27:17", FieldBucket.PREV_PM, 0, listOf(r(31, 31, 24.89, 16), r(32, 32, 22.74, 16), r(33, 33, 18.88, 7))),
            set("2026-06-18T21:27:33", FieldBucket.PREV_PM, 2, listOf(r(19, 19, 10.67, 11), r(21, 21, 10.03, 9), r(20, 20, 12.22, 11))),
            // 2026-06-19 AM (17)
            set("2026-06-19T06:48:53", FieldBucket.AM_RUSH, 0, listOf(r(14, 14, 9.57, 8), r(16, 16, 10.50, 10), r(16, 16, 12.50, 11))),
            set("2026-06-19T06:49:15", FieldBucket.AM_RUSH, 2, listOf(r(16, 16, 13.76, 7), r(15, 15, 13.13, 6), r(15, 15, 13.82, 8))),
            set("2026-06-19T06:49:31", FieldBucket.AM_RUSH, 0, listOf(r(34, 34, 18.34, 9), r(34, 34, 22.12, 12), r(37, 37, 22.35, 14))),
            set("2026-06-19T06:49:47", FieldBucket.AM_RUSH, 0, listOf(r(48, 48, 39.27, 21), r(48, 48, 37.65, 17), r(52, 52, 40.90, 12))),
            set("2026-06-19T06:49:59", FieldBucket.AM_RUSH, 0, listOf(r(49, 49, 30.42, 26), r(49, 49, 28.79, 22), r(54, 54, 34.78, 15))),
            set("2026-06-19T06:50:59", FieldBucket.AM_RUSH, 2, listOf(r(39, 39, 23.94, 14), r(39, 39, 25.57, 18), r(41, 41, 20.98, 10))),
            set("2026-06-19T06:51:11", FieldBucket.AM_RUSH, 0, listOf(r(40, 40, 25.50, 11), r(42, 42, 29.12, 11), r(41, 41, 29.03, 24))),
            set("2026-06-19T06:51:22", FieldBucket.AM_RUSH, 2, listOf(r(49, 49, 36.59, 18), r(50, 50, 39.93, 21), r(49, 49, 37.22, 12))),
            set("2026-06-19T06:51:37", FieldBucket.AM_RUSH, 1, listOf(r(62, 62, 57.03, 18), r(62, 62, 58.65, 22), r(72, 72, 67.19, 15))),
            set("2026-06-19T06:51:48", FieldBucket.AM_RUSH, 0, listOf(r(64, 64, 58.15, 19), r(64, 64, 54.40, 22), r(68, 68, 59.90, 10))),
            set("2026-06-19T06:52:00", FieldBucket.AM_RUSH, 1, listOf(r(52, 52, 42.43, 17), r(55, 55, 42.66, 19), r(56, 56, 47.24, 15))),
            set("2026-06-19T06:52:14", FieldBucket.AM_RUSH, 0, listOf(r(60, 60, 83.33, 11), r(64, 64, 58.90, 11), r(65, 65, 59.30, 25))),
            set("2026-06-19T06:52:35", FieldBucket.AM_RUSH, 0, listOf(r(41, 41, 27.24, 9), r(44, 44, 29.81, 18), r(45, 45, 31.09, 11))),
            set("2026-06-19T06:52:46", FieldBucket.AM_RUSH, 0, listOf(r(104, 104, 168.67, 10), r(118, 118, 160.47, 18), r(114, 114, 172.66, 28))),
            set("2026-06-19T06:52:57", FieldBucket.AM_RUSH, 1, listOf(r(124, 124, 173.37, 19), r(123, 123, 201.00, 12), r(132, 132, 177.96, 14))),
            set("2026-06-19T06:53:09", FieldBucket.AM_RUSH, 2, listOf(r(77, 77, 116.46, 11), r(76, 76, 114.26, 13), r(76, 76, 109.83, 13))),
            set("2026-06-19T06:53:19", FieldBucket.AM_RUSH, 1, listOf(r(18, 18, 18.42, 8), r(18, 18, 17.79, 7), r(20, 20, 18.39, 9))),
            // 2026-06-19 PM (16)
            set("2026-06-19T18:14:23", FieldBucket.PM_RUSH, 1, listOf(r(54, 54, 37.54, 14), r(56, 56, 34.10, 17), r(57, 57, 39.70, 20))),
            set("2026-06-19T18:14:40", FieldBucket.PM_RUSH, 2, listOf(r(74, 74, 59.88, 18), r(80, 80, 62.59, 23), r(86, 86, 95.16, 22))),
            set("2026-06-19T18:14:57", FieldBucket.PM_RUSH, 1, listOf(r(56, 56, 38.53, 13), r(58, 58, 35.34, 14), r(57, 57, 40.94, 22))),
            set("2026-06-19T18:15:09", FieldBucket.PM_RUSH, 2, listOf(r(71, 71, 55.27, 15), r(80, 80, 51.42, 12), r(82, 82, 78.61, 20))),
            set("2026-06-19T18:15:30", FieldBucket.PM_RUSH, 2, listOf(r(62, 62, 46.63, 17), r(64, 64, 47.08, 19), r(67, 67, 43.44, 18))),
            set("2026-06-19T18:15:44", FieldBucket.PM_RUSH, 2, listOf(r(71, 71, 58.47, 16), r(80, 80, 54.74, 16), r(78, 78, 79.27, 22))),
            set("2026-06-19T18:16:02", FieldBucket.PM_RUSH, 1, listOf(r(53, 53, 29.37, 11), r(55, 55, 26.18, 12), r(57, 57, 29.96, 13))),
            set("2026-06-19T18:16:16", FieldBucket.PM_RUSH, 2, listOf(r(46, 46, 25.25, 12), r(45, 45, 27.54, 16), r(50, 50, 23.76, 14))),
            set("2026-06-19T18:16:29", FieldBucket.PM_RUSH, 2, listOf(r(45, 45, 28.13, 13), r(47, 47, 31.94, 16), r(47, 47, 24.95, 14))),
            set("2026-06-19T18:16:42", FieldBucket.PM_RUSH, 1, listOf(r(38, 38, 24.89, 16), r(43, 43, 21.11, 14), r(45, 45, 22.73, 14))),
            set("2026-06-19T18:16:57", FieldBucket.PM_RUSH, 1, listOf(r(19, 19, 14.02, 10), r(21, 21, 17.44, 10), r(21, 21, 16.15, 10))),
            set("2026-06-19T18:17:16", FieldBucket.PM_RUSH, 2, listOf(r(16, 16, 10.87, 7), r(17, 17, 11.60, 11), r(17, 17, 14.29, 7))),
            set("2026-06-19T18:17:35", FieldBucket.PM_RUSH, 1, listOf(r(16, 16, 11.99, 5), r(18, 18, 14.66, 9))),
            set("2026-06-19T18:17:51", FieldBucket.PM_RUSH, 1, listOf(r(19, 19, 10.39, 12), r(19, 19, 11.98, 11), r(19, 19, 12.85, 14))),
            set("2026-06-19T18:18:20", FieldBucket.PM_RUSH, 0, listOf(r(10, 10, 4.45, 4), r(12, 12, 4.64, 5))),
            set("2026-06-19T18:18:38", FieldBucket.PM_RUSH, 0, listOf(r(8, 8, 1.85, 0), r(8, 8, 2.16, 2), r(9, 9, 4.86, 6))),
        )
}
