/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      colors: {
        surface: {
          DEFAULT: '#070b1a',
          card: 'rgba(17,25,46,0.85)',
          elevated: 'rgba(23,33,62,0.9)',
        },
        brand: {
          DEFAULT: '#6aa1ff',
          light: '#8ab5ff',
          dark: '#4a81df',
          purple: '#8a7dff',
        },
        stroke: 'rgba(255,255,255,0.10)',
        muted: '#aeb7d6',
        danger: '#ff6170',
        success: '#2dd79f',
        warning: '#fbbf24',
      },
      backgroundImage: {
        'page-gradient':
          'radial-gradient(circle at 10% 10%, #1b2f62, transparent 45%), radial-gradient(circle at 90% 20%, #402178, transparent 35%)',
      },
      boxShadow: {
        glass: '0 16px 40px rgba(0,0,0,0.36)',
        'glass-sm': '0 4px 16px rgba(0,0,0,0.24)',
        glow: '0 0 20px rgba(106,161,255,0.25)',
      },
      borderRadius: {
        xl2: '1rem',
        xl3: '1.5rem',
      },
      animation: {
        'fade-in': 'fadeIn 0.25s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4,0,0.6,1) infinite',
      },
      keyframes: {
        fadeIn: { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};
