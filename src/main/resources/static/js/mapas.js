// Editor de mapas mentales (Mind Elixir): carga el árbol, lo copia al formulario al guardar,
// aplica los mismos límites que el servidor y avisa si se sale con cambios sin guardar.
(function () {
    const form = document.getElementById('mapa-form');
    const lienzo = document.getElementById('mapa');
    if (!form || !lienzo || window.__mapasInit) {
        return;
    }
    window.__mapasInit = true;

    if (typeof MindElixir === 'undefined') {
        lienzo.textContent = 'No se ha podido cargar el editor de mapas. Recarga la página.';
        lienzo.classList.add('p-4', 'text-error');
        return;
    }
    // El bundle iife expone un objeto { default: clase, SIDE, ... } y no la clase directamente
    const ME = MindElixir.default || MindElixir;

    const MAX_NODOS = Number(lienzo.dataset.maxNodos);
    const MAX_PROFUNDIDAD = Number(lienzo.dataset.maxProfundidad);
    const MAX_TOPIC = Number(lienzo.dataset.maxTopic);

    // Textos del menú contextual (el paquete de idiomas de la librería no va en el bundle iife)
    const ES = {
        addChild: 'Añadir hijo',
        addParent: 'Añadir padre',
        addSibling: 'Añadir hermano',
        removeNode: 'Eliminar nodo',
        focus: 'Modo enfoque',
        cancelFocus: 'Salir del enfoque',
        moveUp: 'Mover arriba',
        moveDown: 'Mover abajo',
        link: 'Enlace',
        linkBidirectional: 'Enlace bidireccional',
        clickTips: 'Haz clic en el nodo de destino',
        summary: 'Resumen',
    };

    // Misma lista blanca que MapaMentalService#normalizarContenido: el servidor es quien decide,
    // pero así lo que llega al DOM de la librería nunca incluye HTML, enlaces ni imágenes.
    function limpiar(nodo) {
        const limpio = { id: String(nodo.id), topic: String(nodo.topic ?? '') };
        if (typeof nodo.expanded === 'boolean') {
            limpio.expanded = nodo.expanded;
        }
        if (nodo.direction === 0 || nodo.direction === 1) {
            limpio.direction = nodo.direction;
        }
        if (Array.isArray(nodo.children)) {
            limpio.children = nodo.children.filter((h) => h && typeof h === 'object').map(limpiar);
        }
        return limpio;
    }

    function datosIniciales() {
        try {
            const datos = JSON.parse(lienzo.dataset.contenido);
            if (datos && datos.nodeData && typeof datos.nodeData === 'object') {
                return { nodeData: limpiar(datos.nodeData) };
            }
        } catch (e) {
            // contenido ilegible: se parte de un mapa vacío con el título como raíz
        }
        return ME.new(document.getElementById('titulo').value || 'Mapa mental');
    }

    const mind = new ME({
        el: lienzo,
        direction: ME.SIDE,
        editable: true,
        toolBar: true,
        keypress: true,
        allowUndo: true,
        newTopicName: 'Nueva idea',
        contextMenu: { focus: true, link: false, locale: ES },
    });
    mind.init(datosIniciales());

    // Nodos y profundidad máxima del árbol actual
    function medir(nodo, nivel) {
        const hijos = nodo.children || [];
        return hijos.reduce(
            (acc, h) => {
                const m = medir(h, nivel + 1);
                return { nodos: acc.nodos + m.nodos, profundidad: Math.max(acc.profundidad, m.profundidad) };
            },
            { nodos: 1, profundidad: nivel }
        );
    }

    // Devuelve el motivo por el que no se puede guardar, o null
    function problema(datos) {
        const { nodos, profundidad } = medir(datos.nodeData, 1);
        if (nodos > MAX_NODOS) {
            return `El mapa tiene ${nodos} nodos (máximo ${MAX_NODOS}).`;
        }
        if (profundidad > MAX_PROFUNDIDAD) {
            return `El mapa tiene ${profundidad} niveles (máximo ${MAX_PROFUNDIDAD}).`;
        }
        const largo = recorrer(datos.nodeData).find((n) => (n.topic || '').trim().length > MAX_TOPIC);
        if (largo) {
            return `Un nodo supera los ${MAX_TOPIC} caracteres: «${largo.topic.trim().slice(0, 30)}…»`;
        }
        return null;
    }

    function recorrer(nodo) {
        return [nodo].concat(...(nodo.children || []).map(recorrer));
    }

    const contador = document.getElementById('mapa-contador');
    function actualizarContador() {
        const { nodos, profundidad } = medir(mind.getData().nodeData, 1);
        contador.textContent = `${nodos} de ${MAX_NODOS} nodos · ${profundidad} de ${MAX_PROFUNDIDAD} niveles`;
    }
    actualizarContador();

    let sinGuardar = false;
    mind.bus.addListener('operation', () => {
        sinGuardar = true;
        actualizarContador();
    });
    // Deshacer y rehacer no emiten 'operation': el contador se refresca cuando cambia el DOM del lienzo
    // (fuera de #mapa, así que no hay bucle). Un solo refresco por fotograma aunque haya muchas mutaciones.
    let contadorPendiente = false;
    new MutationObserver(() => {
        if (!contadorPendiente) {
            contadorPendiente = true;
            requestAnimationFrame(() => {
                contadorPendiente = false;
                actualizarContador();
            });
        }
    }).observe(lienzo, { childList: true, subtree: true });
    form.addEventListener('input', () => (sinGuardar = true));
    window.addEventListener('beforeunload', (e) => {
        if (sinGuardar) {
            e.preventDefault();
        }
    });

    // Exportar: lo que se ve ahora mismo, haya o no cambios sin guardar
    function nombreArchivo() {
        const base = document.getElementById('titulo').value.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
        return base.replace(/[^A-Za-z0-9]+/g, '-').replace(/^-+|-+$/g, '').toLowerCase() || 'mapa-mental';
    }

    document.getElementById('mapa-png').addEventListener('click', async (e) => {
        const boton = e.currentTarget;
        boton.disabled = true;
        try {
            const imagen = await mind.exportPng();
            if (!imagen) {
                throw new Error('sin imagen');
            }
            const url = URL.createObjectURL(imagen);
            const enlace = Object.assign(document.createElement('a'), { href: url, download: nombreArchivo() + '.png' });
            enlace.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (err) {
            ToastSystem.mostrar('error', 'No se ha podido generar la imagen. Prueba con Imprimir.');
        } finally {
            boton.disabled = false;
        }
    });

    // Acciones táctiles: la librería solo las ofrece con teclado o clic derecho
    function nodoActual() {
        const nodo = mind.currentNode;
        if (!nodo) {
            ToastSystem.mostrar('warning', 'Toca primero un nodo del mapa.');
        }
        return nodo;
    }
    const acciones = {
        hijo: () => nodoActual() && mind.addChild(),
        hermano: () => {
            const nodo = nodoActual();
            if (!nodo) return;
            if (nodo.nodeObj.root) {
                ToastSystem.mostrar('warning', 'El nodo central no puede tener hermanos: añade un hijo.');
                return;
            }
            mind.insertSibling('after');
        },
        editar: () => nodoActual() && mind.beginEdit(),
        eliminar: () => {
            const nodo = nodoActual();
            if (!nodo) return;
            if (nodo.nodeObj.root) {
                ToastSystem.mostrar('warning', 'El nodo central no se puede eliminar.');
                return;
            }
            mind.removeNodes(mind.currentNodes || [nodo]);
        },
        deshacer: () => mind.undo(),
        rehacer: () => mind.redo(),
    };
    document.getElementById('mapa-tactil')?.addEventListener('click', (e) => {
        const boton = e.target.closest('button[data-accion]');
        if (boton) {
            acciones[boton.dataset.accion]();
        }
    });

    // Pantalla completa: nativa si el navegador la ofrece y, si no (iPhone, algunos móviles), solo la capa CSS.
    // La clase se aplica siempre, así el lienzo gana espacio en ambos casos.
    const seccion = document.getElementById('mapa-seccion');
    function pantallaCompleta(activa) {
        seccion.classList.toggle('pantalla-completa', activa);
        document.documentElement.classList.toggle('mapa-bloqueado', activa);
        requestAnimationFrame(() => mind.toCenter());
    }
    async function alternarPantallaCompleta() {
        const activa = !seccion.classList.contains('pantalla-completa');
        pantallaCompleta(activa);
        try {
            if (activa && document.fullscreenEnabled) {
                await seccion.requestFullscreen();
            } else if (!activa && document.fullscreenElement) {
                await document.exitFullscreen();
            }
        } catch (err) {
            // sin API nativa o rechazada: queda la capa CSS
        }
    }
    // Esc u otros gestos del navegador sacan de la pantalla nativa sin pasar por el botón
    document.addEventListener('fullscreenchange', () => {
        if (!document.fullscreenElement && seccion.classList.contains('pantalla-completa')) {
            pantallaCompleta(false);
        }
    });
    const botonCompleta = document.getElementById('fullscreen');
    if (botonCompleta) {
        botonCompleta.onclick = alternarPantallaCompleta;
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && seccion.classList.contains('pantalla-completa') && !e.target.closest('#input-box')) {
            pantallaCompleta(false);
        }
    });

    document.getElementById('mapa-imprimir').addEventListener('click', () => window.print());

    // Un solo envío; el árbol se copia al campo oculto justo antes de enviar
    let enviando = false;
    form.addEventListener('submit', (e) => {
        // El borrado no necesita el árbol ni validarlo
        if (e.submitter && e.submitter.hasAttribute('data-confirm')) {
            sinGuardar = false;
            return;
        }
        // El mazo se crea con el mapa guardado: con cambios pendientes se avisa en vez de usar una versión vieja
        if (e.submitter && e.submitter.hasAttribute('data-crear-mazo')) {
            if (sinGuardar) {
                e.preventDefault();
                e.stopPropagation(); // que loading-state.js no deje el botón en "Creando mazo..."
                ToastSystem.mostrar('warning', 'Guarda los cambios antes de crear el mazo: se crea con el mapa guardado.');
            } else {
                // Sale de esta página: no debe saltar el aviso de cambios sin guardar
                sinGuardar = false;
            }
            return;
        }
        if (enviando) {
            e.preventDefault();
            return;
        }
        const datos = mind.getData();
        const motivo = problema(datos);
        if (motivo) {
            e.preventDefault();
            // loading-state.js escucha en document y deshabilitaría el botón aunque no se envíe nada
            e.stopPropagation();
            ToastSystem.mostrar('error', motivo + ' Corrígelo para poder guardar el mapa.');
            return;
        }
        document.getElementById('contenido').value = JSON.stringify({ nodeData: datos.nodeData });
        enviando = true;
        sinGuardar = false;
    });

    // Ctrl/Cmd+S guarda, salvo mientras se edita el texto de un nodo
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
            e.preventDefault();
            if (e.target.closest('#input-box')) {
                return;
            }
            form.requestSubmit(document.getElementById('mapa-guardar'));
        }
    });

    // Borrado con confirmación (confirmDialog es un `let` global de confirm-dialog.js)
    document.addEventListener('click', async (e) => {
        const boton = e.target.closest('button[data-confirm]');
        if (!boton || boton.dataset.confirmado === '1') {
            return;
        }
        e.preventDefault();
        if (typeof confirmDialog === 'undefined') {
            return;
        }
        const resultado = await confirmDialog.open({
            title: '¿Eliminar?',
            message: boton.dataset.confirm,
            confirmBtnText: 'Eliminar',
            type: 'danger',
            icon: 'fa-regular fa-trash-can',
        });
        if (resultado.isConfirmed) {
            boton.dataset.confirmado = '1';
            boton.form.requestSubmit(boton);
        }
    });
})();
