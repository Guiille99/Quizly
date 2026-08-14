let confirmDialog;

document.addEventListener('DOMContentLoaded', () => {

    const dialog = document.getElementById('confirm-dialog');

    if (!dialog) {
        console.warn('No se ha encontrado el elemento #confirm-dialog');
        return;
    }

    const titleElement =
        document.getElementById('confirm-dialog-title');

    const messageElement =
        document.getElementById('confirm-dialog-message');

    const confirmButton =
        document.getElementById('confirm-dialog-confirm');

    const cancelButton =
        document.getElementById('confirm-dialog-cancel');

    const iconContainer =
        document.getElementById('confirm-dialog-icon-container');

    const iconElement =
        document.getElementById('confirm-dialog-icon');


    const TYPES = {
        danger: {
            iconContainer: 'bg-error/10 text-error',
            button: 'btn-error'
        },

        warning: {
            iconContainer: 'bg-warning/10 text-warning',
            button: 'btn-warning'
        },

        info: {
            iconContainer: 'bg-info/10 text-info',
            button: 'btn-info'
        },

        success: {
            iconContainer: 'bg-success/10 text-success',
            button: 'btn-success'
        }
    };


    const DEFAULT_OPTIONS = {
        title: '¿Estás seguro?',
        message: '',
        confirmBtnText: 'Confirmar',
        cancelBtnText: 'Cancelar',
        icon: 'fa-solid fa-triangle-exclamation',
        type: 'warning'
    };


    function applyType(type) {

        const config = TYPES[type] ?? TYPES.warning;

        Object.values(TYPES).forEach(typeConfig => {

            iconContainer.classList.remove(
                ...typeConfig.iconContainer.split(' ')
            );

            confirmButton.classList.remove(
                typeConfig.button
            );
        });

        iconContainer.classList.add(
            ...config.iconContainer.split(' ')
        );

        confirmButton.classList.add(config.button);
    }


    function applyIcon(icon) {
        iconElement.className = `${icon} text-2xl`;
    }


    function open(options = {}) {

        const config = {
            ...DEFAULT_OPTIONS,
            ...options
        };

        titleElement.textContent = config.title;
        messageElement.textContent = config.message;

        confirmButton.textContent = config.confirmBtnText;
        cancelButton.textContent = config.cancelBtnText;

        applyIcon(config.icon);
        applyType(config.type);

        return new Promise(resolve => {
            let resolved = false;

            const resolveDialog = result => {
                if (resolved) {
                    return;
                }

                resolved = true;

                cleanup();

                if (dialog.open) {
                    dialog.close();
                }

                resolve(result);
            };

            const handleConfirm = () => {
                // dialog.close();
                // cleanup();

                resolveDialog({
                    isConfirmed: true,
                    isDimissed: false
                });
            };

            const handleCancel = () => {
                // dialog.close();
                // cleanup();

                resolveDialog({
                    isConfirmed: false,
                    isDimissed: true
                });
            };

            const handleClose = () => {
                resolveDialog({
                    isConfirmed: false,
                    isDimissed: true
                });
            };

            function cleanup() {
                confirmButton.removeEventListener('click', handleConfirm);
                cancelButton.removeEventListener('click', handleCancel);
                dialog.removeEventListener('close', handleClose);
            }

            confirmButton.addEventListener('click', handleConfirm);
            cancelButton.addEventListener('click', handleCancel);
            dialog.addEventListener('close', handleClose);
            
            dialog.showModal();
        });

    }


    function close() {
        dialog.close();
    }


    confirmDialog = {
        open,
        close
    };
});