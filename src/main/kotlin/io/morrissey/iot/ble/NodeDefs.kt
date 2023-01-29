package io.morrissey.iot.ble

/**
 * A list of the currently allowed nodes. This could be augmented with a dynamic storage of these values along with
 * the ability to add newly discovered values once verified.
 */
val knownNodeDefs = listOf(
    NodeDef("98:F4:AB:80:97:86", NodeStatus.DEPLOYED),
    NodeDef("E8:DB:84:02:BD:12", NodeStatus.TESTING)
)
