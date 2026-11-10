interface IconProps {
  className?: string;
}

const common = {
  width: 20,
  height: 20,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
};

export function IconClose({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <line x1="5" y1="5" x2="19" y2="19" />
      <line x1="19" y1="5" x2="5" y2="19" />
    </svg>
  );
}

export function IconArchive({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <rect x="3" y="4" width="18" height="5" rx="1" />
      <path d="M5 9v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9" />
      <line x1="10" y1="13" x2="14" y2="13" />
    </svg>
  );
}

export function IconCalendar({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <rect x="3.5" y="5" width="17" height="16" rx="1.5" />
      <line x1="3.5" y1="9.5" x2="20.5" y2="9.5" />
      <line x1="8" y1="3" x2="8" y2="7" />
      <line x1="16" y1="3" x2="16" y2="7" />
    </svg>
  );
}

export function IconCheck({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <path d="M4 12.5l5 5L20 6" />
    </svg>
  );
}

export function IconLogout({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <polyline points="16 17 21 12 16 7" />
        <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  );
}

export function IconUser({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <circle cx="12" cy="8" r="3.5" />
      <path d="M4.5 20c1.4-3.6 4.4-5.5 7.5-5.5s6.1 1.9 7.5 5.5" />
    </svg>
  );
}

export function IconUsers({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <circle cx="9" cy="8.5" r="3.2" />
      <path d="M2.5 19.5c1.1-3.2 3.7-4.9 6.5-4.9s5.4 1.7 6.5 4.9" />
      <path d="M15.5 5.6a3.2 3.2 0 0 1 0 5.8" />
      <path d="M17.8 14.9c1.8.6 3.1 2.2 3.7 4.6" />
    </svg>
  );
}

export function IconList({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <rect x="4.5" y="3.5" width="15" height="17" rx="1.5" />
      <line x1="8.5" y1="8.5" x2="15.5" y2="8.5" />
      <line x1="8.5" y1="12" x2="15.5" y2="12" />
      <line x1="8.5" y1="15.5" x2="13" y2="15.5" />
    </svg>
  );
}

export function IconBox({ className }: IconProps) {
  return (
    <svg {...common} className={className} aria-hidden="true">
      <path d="M3.5 7.5 12 3l8.5 4.5L12 12 3.5 7.5Z" />
      <path d="M3.5 7.5V16L12 20.5V12" />
      <path d="M20.5 7.5V16L12 20.5" />
    </svg>
  );
}
