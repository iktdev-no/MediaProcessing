import type { JSX } from "react"

export type MenuItem = {
    id: string
    label: string
    icon: JSX.Element
    onClick: () => void
}

export type MenuLayout = {
    topItems: MenuItem[]
    bottomItems: MenuItem[]
}