// @ts-check
import {themes as prismThemes} from 'prism-react-renderer';

// One source of truth: the guides live in ../docs, so the same .md files render on GitHub and
// on this site. The site is served at https://23daniel3.github.io/Ciprix/docs/ (gh-pages/docs).
/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Ciprix',
  tagline: 'State machines and requests for FRC robots',
  favicon: 'img/favicon.svg',

  url: 'https://23daniel3.github.io',
  baseUrl: '/Ciprix/docs/',
  organizationName: '23Daniel3',
  projectName: 'Ciprix',
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',

  // The guides are plain CommonMark that must also read well on GitHub (things like
  // `<request>` outside code would break MDX), so .md files are not parsed as MDX.
  markdown: {
    format: 'detect',
    hooks: {onBrokenMarkdownLinks: 'throw'},
  },

  i18n: {defaultLocale: 'en', locales: ['en']},

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          path: '../docs',
          routeBasePath: '/',
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/23Daniel3/Ciprix/edit/main/docs/',
        },
        blog: false,
        theme: {customCss: './src/css/custom.css'},
      }),
    ],
  ],

  themes: [
    [
      '@easyops-cn/docusaurus-search-local',
      /** @type {import("@easyops-cn/docusaurus-search-local").PluginOptions} */
      ({
        hashed: true,
        indexBlog: false,
        docsRouteBasePath: '/',
        docsDir: '../docs',
        language: ['en'],
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {respectPrefersColorScheme: true},
      navbar: {
        title: 'Ciprix',
        logo: {alt: 'Ciprix logo', src: 'img/ciprix-logo-icon.png'},
        items: [
          {type: 'docSidebar', sidebarId: 'guides', position: 'left', label: 'Documentation'},
          {to: '/credits', label: 'Credits', position: 'left'},
          {href: 'https://github.com/23Daniel3/Ciprix/releases', label: 'Releases', position: 'right'},
          {href: 'https://github.com/23Daniel3/Ciprix', label: 'GitHub', position: 'right'},
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Ciprix',
            items: [
              {label: 'Getting started', to: '/getting-started'},
              {label: 'Release notes', to: '/release-notes'},
              {label: 'GitHub', href: 'https://github.com/23Daniel3/Ciprix'},
            ],
          },
          {
            title: 'Legal',
            items: [
              {label: 'Credits and licenses', to: '/credits'},
              {label: 'GPLv3 license', href: 'https://github.com/23Daniel3/Ciprix/blob/main/LICENSE'},
              {label: 'NOTICE (derived code)', href: 'https://github.com/23Daniel3/Ciprix/blob/main/NOTICE.md'},
            ],
          },
        ],
        copyright:
          'Ciprix is licensed under GPLv3 · Built with Docusaurus · Documentation style inspired by AdvantageKit (FRC 6328)',
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['java', 'groovy', 'json', 'bash'],
      },
    }),
};

export default config;
