import { initTheme } from './modules/theme.js';
import { initNavigation } from './modules/navigation.js';
import { initForms } from './modules/forms.js';
import { initSearch } from './modules/search.js';
import { initAdmin } from './modules/admin.js';
import { initProfileMedia } from './modules/profile-media.js';

// Cada módulo verifica por conta própria se os elementos que precisa
// existem na página atual. Nenhum módulo deve lançar erro quando o
// componente correspondente não estiver presente.
initTheme();
initNavigation();
initForms();
initSearch();
initAdmin();
initProfileMedia();
