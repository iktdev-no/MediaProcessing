const listeners = new Map<string, Set<(data: any) => void>>()

export function subscribe(eventName: string, handler: (data: any) => void) {
    if (!listeners.has(eventName)) {
        listeners.set(eventName, new Set())
    }
    listeners.get(eventName)!.add(handler)

    return () => {
        listeners.get(eventName)!.delete(handler)
        // viktig: ikke returner noe
    }
}

export function dispatch(eventName: string, data: any) {
    //console.debug(`EventBus: dispatching event "${eventName}"`, data)
    listeners.get(eventName)?.forEach(handler => handler(data))
}
