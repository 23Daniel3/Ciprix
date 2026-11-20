// @ts-check
/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  guides: [
    'index',
    {type: 'doc', id: 'getting-started', label: 'Getting started'},
    {type: 'doc', id: 'architecture', label: 'Core concepts'},
    {type: 'doc', id: 'hardware-power-and-pit', label: 'Hardware'},
    {type: 'category', label: 'Driving', items: ['controllers', 'driving-helpers', 'zones']},
    {type: 'doc', id: 'safety', label: 'Safety'},
    {type: 'doc', id: 'vision', label: 'Vision'},
    {type: 'doc', id: 'simulation', label: 'Simulation & replay'},
    {type: 'doc', id: 'tunables-and-dashboard', label: 'Tunables'},
    {type: 'doc', id: '3d-robot', label: '3D robot'},
    {type: 'doc', id: 'migrating-to-wpilib-2027', label: 'Migrating to 2027'},
    {type: 'doc', id: 'release-notes', label: 'Release notes'},
    'credits',
  ],
};

export default sidebars;
