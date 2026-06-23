package com.example.petquest.data.model

/**
 * All task pool content for PetQuest's rotating daily task system.
 *
 * Every string uses {name} as a placeholder — replaced at generation time
 * with the pet's actual name. Pool sizes are chosen to ensure meaningful
 * day-to-day variety:
 *
 *   - UNIVERSAL_CORE:    10 tasks → 2 drawn daily   → repeats after ~45 days minimum
 *   - virtue pools:       8 tasks → 2 drawn daily   → repeats after ~28 days minimum
 *   - UNIVERSAL_OPTIONAL: 12 tasks → 4 drawn daily  → 495 combinations, ~124 days
 *   - species optionals:  2–3 extra tasks mixed in for relevant species
 *
 * Task selection is deterministic per (petId, date) pair — same tasks all day,
 * different tasks tomorrow. See PetQuestViewModel.generateTasksForPet().
 */
object TaskPools {

    // ─── Universal Core Pool (10 tasks — draw 2 daily) ────────────────────────
    // Applied to every pet regardless of species or virtue.

    val UNIVERSAL_CORE = listOf(
        "Feed {name}",
        "Give fresh water to {name}",
        "Check {name} is comfortable and not stressed",
        "Spend 5 minutes of quality time with {name}",
        "Give {name} a quick health check (eyes, coat, posture)",
        "Make sure {name}'s living space is clean and safe",
        "Refresh {name}'s bedding or resting area",
        "Observe {name}'s behaviour for anything unusual today",
        "Talk to or interact with {name} for a few minutes",
        "Make sure {name} has everything they need to be happy"
    )

    // ─── Per-Virtue Core Pools (8 tasks each — draw 2 daily) ──────────────────
    // Selected based on the pet's dominant virtue.

    private val WISDOM_CORE = listOf(
        "Let {name} explore a safe new space",
        "Introduce {name} to a new safe object to investigate",
        "Offer {name} a puzzle or enrichment activity",
        "Rearrange {name}'s environment slightly for new stimulation",
        "Hide a treat somewhere for {name} to find",
        "Give {name} something with a new texture or scent to investigate",
        "Let {name} observe something new from a safe distance",
        "Create a foraging or searching challenge for {name}"
    )

    private val DILIGENCE_CORE = listOf(
        "Stick to {name}'s regular feeding schedule today",
        "Complete a full routine check on {name}'s health and environment",
        "Clean and tidy {name}'s space thoroughly",
        "Play with {name} for a full 10 minutes without distractions",
        "Check and replenish all of {name}'s supplies",
        "Log a note about {name}'s behaviour or health today",
        "Spend focused, uninterrupted time with {name}",
        "Review and refresh {name}'s enrichment setup"
    )

    private val TEMPERANCE_CORE = listOf(
        "Create a cosy, comfortable rest spot for {name}",
        "Spend calm, quiet time sitting beside {name}",
        "Give {name} a gentle pet and a cuddle",
        "Let {name} sleep undisturbed for a while",
        "Provide {name} with a warm, comfortable area to relax",
        "Make sure {name} isn't being overstimulated today",
        "Offer {name} a slow, gentle, relaxing activity",
        "Sit quietly with {name} without any distractions"
    )

    private val COURAGE_CORE = listOf(
        "Introduce {name} to something new they haven't encountered before",
        "Redirect {name}'s energy into a bold, active challenge",
        "Set up a safe space where {name} can explore freely",
        "Give {name} an enrichment puzzle or challenge to solve",
        "Provide {name} something to climb, rearrange, or conquer",
        "Channel {name}'s spirit into a taught trick or new skill",
        "Give {name} a foraging or hiding-and-seeking game",
        "Try something energetic and adventurous with {name}"
    )

    private val COMPASSION_CORE = listOf(
        "Spend quality social time with {name}",
        "Let {name} greet you warmly and enthusiastically",
        "Practise a friendly behaviour or a fun trick with {name}",
        "Give {name} attention in a social and interactive setting",
        "Reward {name} for their good social behaviour today",
        "Invite someone {name} knows and likes to spend time with them",
        "Take {name} somewhere new to experience new sights and sounds",
        "Give {name} a long, affectionate cuddle or grooming session"
    )

    fun virtueCorePool(virtue: Virtue): List<String> = when (virtue) {
        Virtue.WISDOM      -> WISDOM_CORE
        Virtue.DILIGENCE   -> DILIGENCE_CORE
        Virtue.TEMPERANCE  -> TEMPERANCE_CORE
        Virtue.COURAGE     -> COURAGE_CORE
        Virtue.COMPASSION  -> COMPASSION_CORE
    }

    // ─── Universal Optional Pool (12 tasks — combined with species pool, draw 4) ─

    val UNIVERSAL_OPTIONAL = listOf(
        "Brush or groom {name}",
        "Give {name} a special treat today",
        "Check and trim {name}'s nails if needed",
        "Clean {name}'s living area thoroughly",
        "Record a short note about {name}'s mood today",
        "Weigh {name} and note any recent changes",
        "Take a photo of {name} to remember today",
        "Check {name}'s coat or skin for anything unusual",
        "Replace or wash {name}'s bedding",
        "Give {name} some outdoor or window time today",
        "Research one new fact about {name}'s species",
        "Check {name}'s equipment or accessories for wear"
    )

    // ─── Species-Specific Optional Pools (2–3 tasks per species) ──────────────
    // Combined with universal optionals before drawing 4 daily optional tasks.
    // Species with no entry fall back to universal optionals only.

    private val SPECIES_OPTIONAL_POOLS = mapOf(
        PetType.FISH to listOf(
            "Clean {name}'s tank or do a partial water change",
            "Check the filter, heater, and temperature for {name}",
            "Test {name}'s water parameters"
        ),
        PetType.TURTLE to listOf(
            "Give {name} supervised time outside the tank",
            "Check {name}'s shell for any cracks or irregularities",
            "Make sure {name} gets adequate UV light time today"
        ),
        PetType.BIRD to listOf(
            "Let {name} out for supervised flight time",
            "Talk or whistle with {name} for a few minutes",
            "Clean {name}'s cage perches and toys"
        ),
        PetType.HORSE to listOf(
            "Groom {name}'s mane and tail thoroughly",
            "Check {name}'s hooves for stones or damage",
            "Give {name} adequate exercise or turnout time"
        ),
        PetType.SNAKE to listOf(
            "Check {name}'s enclosure temperature and humidity",
            "Monitor {name}'s shedding progress if applicable",
            "Verify {name}'s hides and secure areas are in place"
        ),
        PetType.HAMSTER to listOf(
            "Clean {name}'s exercise wheel",
            "Spot-clean and refresh {name}'s bedding",
            "Give {name} safe supervised time in the exercise ball"
        ),
        PetType.GUINEA_PIG to listOf(
            "Give {name} fresh vegetables as a healthy supplement",
            "Let {name} run freely in a safe floor area",
            "Check {name}'s teeth and nails"
        ),
        PetType.RABBIT to listOf(
            "Give {name} fresh hay and check the supply",
            "Let {name} run and binky in a safe open space",
            "Check {name}'s ears for any debris or irregularities"
        ),
        PetType.LIZARD to listOf(
            "Check {name}'s basking and ambient temperatures",
            "Mist or check humidity levels for {name} if required",
            "Observe {name}'s appetite and skin colour today"
        ),
        PetType.FROG to listOf(
            "Check {name}'s enclosure humidity and moisture",
            "Clean {name}'s water dish or water area",
            "Observe {name}'s skin condition today"
        ),
        PetType.OWL to listOf(
            "Give {name} an enrichment activity or feeding challenge",
            "Check {name}'s feathers for any sign of damage",
            "Ensure {name}'s perches are secure and clean"
        ),
        PetType.PENGUIN to listOf(
            "Check {name}'s water quality and temperature",
            "Give {name} a feeding interaction today",
            "Observe {name}'s swimming or movement"
        ),
        PetType.PANDA to listOf(
            "Provide {name} with fresh enrichment or browse today",
            "Check {name}'s enclosure for any hazards",
            "Give {name} a mental challenge or foraging activity"
        ),
        PetType.MONKEY to listOf(
            "Give {name} an enrichment puzzle or hanging toy",
            "Observe {name}'s social behaviour and mood today",
            "Provide {name} with fresh browse or foraging material"
        ),
        PetType.FOX to listOf(
            "Give {name} a digging or foraging opportunity",
            "Check {name}'s enclosure boundaries for security",
            "Provide {name} with a new mental enrichment activity"
        ),
        PetType.BEAR to listOf(
            "Provide {name} a large enrichment item or puzzle today",
            "Observe {name}'s activity level and energy",
            "Ensure {name}'s space is properly secured and stimulating"
        ),
        PetType.WOLF to listOf(
            "Give {name} a scent enrichment or tracking activity",
            "Observe {name}'s energy levels and behaviour today",
            "Provide {name} with a large foraging or hunting challenge"
        ),
        PetType.DEER to listOf(
            "Check {name}'s hooves and legs",
            "Provide {name} with natural browse or a browse substitute",
            "Observe {name}'s movement and gait for anything unusual"
        ),
        PetType.DUCK to listOf(
            "Check {name}'s water area is clean and accessible",
            "Give {name} supervised swimming or bathing time",
            "Check {name}'s bill and feet for any issues"
        ),
        PetType.CRAB to listOf(
            "Check {name}'s enclosure salinity and moisture levels",
            "Provide {name} with a fresh hiding or burrowing spot",
            "Observe {name}'s moulting progress if applicable"
        ),
        PetType.COW to listOf(
            "Check {name}'s hooves and legs",
            "Give {name} fresh water and check grazing access",
            "Observe {name}'s general health and demeanour"
        ),
        PetType.PIG to listOf(
            "Give {name} a rooting enrichment or a forage box",
            "Check {name}'s skin for any dryness or irritation",
            "Give {name} mud time or a wallow if available"
        ),
        PetType.GOAT to listOf(
            "Check {name}'s hooves for trimming needs",
            "Give {name} enrichment to climb or explore safely",
            "Ensure {name}'s mineral lick is available and accessible"
        ),
        PetType.SHEEP to listOf(
            "Check {name}'s fleece for any matting or parasites",
            "Observe {name}'s movement and gait",
            "Ensure {name} has access to shade or appropriate shelter"
        ),
        PetType.CHICKEN to listOf(
            "Clean {name}'s coop or housing area",
            "Check {name}'s feathers and vent area",
            "Give {name} scratch grain or treats to forage"
        ),
        PetType.FERRET to listOf(
            "Give {name} a tunnelling or digging enrichment activity",
            "Check {name}'s ears for any wax buildup",
            "Let {name} explore safely outside their enclosure"
        ),
        PetType.HEDGEHOG to listOf(
            "Check and clean {name}'s exercise wheel",
            "Observe {name}'s quills and skin health",
            "Give {name} a foraging tray or digging box"
        )
    )

    fun speciesOptionalPool(type: PetType): List<String> =
        SPECIES_OPTIONAL_POOLS[type] ?: emptyList()
}
