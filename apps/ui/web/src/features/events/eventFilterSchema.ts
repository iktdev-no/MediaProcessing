export const eventFilterSchema = {
    // eventId er typisk en streng som "UserCreated", "OrderPlaced"
    eventIds: [] as string[], // du fyller inn fra backend senere

    // keys (samme som tasks)
    keyLabel: "Key",

    // date filters
    dateFilters: ["from:", "to:"]
}
