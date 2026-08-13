// Restricciones de los tipos de preguntas
const QUESTIONS_RULES = {
    SINGLE_CHOICE:{
        minAnswer: 2,
        maxAnswer: 4
    },
    TRUE_FALSE: {
        minAnswer: 2,
        maxAnswer: 2
    }
};

const availableQuestionTypes = quizFormData?.tiposPregunta ?? [];
const initialQuestions = mapInitialQuestions(quizFormData?.preguntas ?? []);
let questions = initialQuestions;;

const answersContainer = document.getElementById('answers-container');
const addAnswerButton = document.getElementById('add-answer-btn');
const saveQuestionButton = document.getElementById('save-question-btn');
const questionsList = document.getElementById('questions-list');
const questionsEmptyState = document.getElementById('questions-empty-state');
const questionsListBody = document.getElementById('questions-list-body');
const questionType = document.getElementById('question-type');
const questionsFormData = document.getElementById('questions-form-data');

const answerLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

// FUNCIONES

function mapInitialQuestions(preguntas) {
    return preguntas.map(pregunta => {
        const tipoPregunta = availableQuestionTypes.find(
            tipo => tipo.codigo === pregunta.tipoPreguntaCodigo
        );

        return {
            enunciado: pregunta.enunciado,
            tipoPregunta: tipoPregunta,
            opcionesRespuesta: pregunta.opcionesRespuesta
        }
    });
}

function openQuestionDialog() {
    resetQuestionDialog();
    questionDialog.showModal();
}

function openEditQuestionDialog(index) {
    if (index < 0 || index >= questions.length) {
        return;
    }

    const question = questions[index];

    questionDialog.dataset.questionIndex = index;

    loadQuestionIntoDialog(question);

    questionDialog.showModal();
}

function resetQuestionDialog() {
    /*
     * El modal se abre siempre inicialmente en modo creación.
     */
    setQuestionDialogMode('create');

    // Eliminamos el índice del dialog
    delete questionDialog.dataset.questionIndex;

    /*
     * Restablecer enunciado.
     */
    const questionText = document.getElementById('question-text');

    if (questionText) {
        questionText.value = '';
    }

    /*
     * Restablecer tipo de pregunta.
     */
    if (questionType) {
        questionType.value = 'SINGLE_CHOICE';
    }

    /*
     * Restaurar las respuestas iniciales.
     */
    answersContainer.innerHTML = '';

    for (let i = 0; i < QUESTIONS_RULES[questionType.value].minAnswer; i++) {
        answersContainer.appendChild(createAnswer());
    }

    // Restauramos el botón de añadir respuesta
    addAnswerButton?.classList.remove('hidden');

    updateAnswerLetters();
    updateAddAnswerBtn();
    clearQuestionErrors();
}

function updateAnswerLetters() {
    const answers = answersContainer.querySelectorAll('[data-answer]');

    answers.forEach((answer, index) => {
        const letter = answer.querySelector('.answer-letter');

        if (letter) {
            letter.textContent = answerLetters[index];
        }
    });
}

function createAnswer() {
    const answer = document.createElement('div');

    answer.className = 'flex items-start gap-2 bg-base-200 rounded p-4';
    answer.dataset.answer = '';

    answer.innerHTML = `
        <div class="answer-letter flex items-center justify-center
                    w-8 h-8 shrink-0
                    rounded-md
                    bg-primary/10
                    text-primary
                    font-semibold text-sm">
            A
        </div>

        <div class="flex-1 min-w-0">
            <input type="text"
                class="input w-full"
                data-answer-text
                maxlength="255"
                placeholder="Texto de la respuesta...">
        
            <textarea 
                class="textarea textarea-sm w-full mt-2"
                data-answer-feedback
                maxlength="500" 
                placeholder="Retroalimentación opcional..."></textarea>
        </div>

        <div class="flex items-center gap-1 self-start mt-1">
            <label class="cursor-pointer"
                title="Marcar como respuesta correcta">

                <input type="radio"
                    name="question-correct-answer"
                    class="radio radio-primary radio-sm"
                    data-answer-correct>

            </label>

            <button type="button"
                    class="btn btn-ghost btn-square btn-sm text-error"
                    data-remove-answer
                    title="Eliminar respuesta">

                <i class="fa-regular fa-trash-can"></i>

            </button>
        </div>
    `;

    return answer;
}

function addAnswer() {
    

    const answer = createAnswer();

    answersContainer.appendChild(answer);

    updateAnswerLetters();

    answer.querySelector('[data-answer-text]')?.focus();

    updateAddAnswerBtn();
}

function removeAnswer(answer) {
    const answers = answersContainer.querySelectorAll('[data-answer]');

    /*
     * No permitimos eliminar si solo quedan dos respuestas.
     */
    if (answers.length <= 2) {
        return;
    }

    const wasCorrect = answer
        .querySelector('[data-answer-correct]')
        ?.checked;

    answer.remove();

    updateAnswerLetters();

    updateAddAnswerBtn();

    /*
     * Si eliminamos la respuesta correcta, no seleccionamos
     * automáticamente otra. El usuario deberá seleccionar
     * explícitamente la nueva respuesta correcta.
     */
    if (wasCorrect) {
        // No hacemos nada.
    }
}

function showError(element, message) {
    if (!element) {
        return;
    }

    element.textContent = message;
    element.classList.remove('hidden');
}

function hideError(element) {
    if (!element) {
        return;
    }

    element.textContent = '';
    element.classList.add('hidden');
}

function clearQuestionErrors() {
    hideError(document.getElementById('question-text-error'));
    hideError(document.getElementById('answers-error'));

    document
        .getElementById('question-text')
        ?.classList.remove('input-error');

    answersContainer
        ?.classList.remove('border-error');
}

function validateQuestion() {
    clearQuestionErrors();

    let valid = true;

    const questionText =
        document.getElementById('question-text');

    const questionTextError =
        document.getElementById('question-text-error');

    const answersError =
        document.getElementById('answers-error');

    const errors = [];


    /*
     * Enunciado.
     */
    if (!questionText.value.trim()) {
        showError(
            questionTextError,
            'El enunciado de la pregunta es obligatorio.'
        );

        questionText.classList.add('input-error');

        valid = false;
    }


    /*
     * Respuestas.
     */
    const answers =
        [...answersContainer.querySelectorAll('[data-answer]')];


    if (answers.length < QUESTIONS_RULES[questionType.value].minAnswer) {
        errors.push(
            `La pregunta debe tener al menos ${QUESTIONS_RULES[questionType.value].minAnswer} opciones.`
        );
    }

    if (answers.length > QUESTIONS_RULES[questionType.value].maxAnswer) {
        errors.push(`La pregunta no puede tener más de ${QUESTIONS_RULES[questionType.value].maxAnswer} opciones`)
    }


    const hasEmptyAnswer = answers.some(answer => {
        const input =
            answer.querySelector('[data-answer-text]');

        return !input?.value.trim();
    });

    if (hasEmptyAnswer) {
        errors.push(
            'Todas las respuestas deben tener un texto.'
        );
    }


    const correctAnswers =
        answers.filter(answer => {
            const radio =
                answer.querySelector('[data-answer-correct]');

            return radio?.checked;
        });


    if (correctAnswers.length === 0) {
        errors.push(
            'Debes seleccionar una respuesta correcta.'
        );
    }


    if (errors.length > 0) {
        showError(
            answersError,
            errors.join(' ')
        );

        valid = false;
    }


    return valid;
}

function getQuestionFromDialog() {
    const questionText =
        document.getElementById('question-text');

    const answers =
        [...answersContainer.querySelectorAll('[data-answer]')];

    return {
        enunciado: questionText.value.trim(),

        tipoPregunta: {
            codigo: questionType.value,
            nombre: questionType.selectedOptions[0].text
        },

        opcionesRespuesta: answers.map(answer => {
            const text =
                answer
                    .querySelector('[data-answer-text]')
                    .value
                    .trim();

            const correct =
                answer
                    .querySelector('[data-answer-correct]')
                    .checked;

            const feddback =
                answer
                    .querySelector('[data-answer-feedback]')
                    ?.value
                    .trim() || null;

            return {
                texto: text,
                correcta: correct,
                retroalimentacion: feddback
            };
        })
    };
}

function updateAddAnswerBtn() {
    const answers = answersContainer.querySelectorAll('[data-answer]');

    if (answers.length >= QUESTIONS_RULES[questionType.value].maxAnswer) {
        addAnswerButton.setAttribute("disabled", "true");
    } else{
        addAnswerButton.removeAttribute("disabled")
    }
}

function createQuestionRow(question, index) {
    const row = document.createElement('tr');

    row.dataset.questionIndex = index;

    const correctAnswerIndex =
        question.opcionesRespuesta.findIndex(
            opcion => opcion.correcta
        );

    row.innerHTML = `
        <td>
            ${index + 1}
        </td>

        <td>
            <span class="font-medium">
                ${escapeHtml(question.enunciado)}
            </span>
        </td>

        <td>
            <span class="badge badge-muted h-fit">
                ${escapeHtml(question.tipoPregunta.nombre)}
            </span>
        </td>

        <td>
            ${question.opcionesRespuesta.length}
        </td>

        <td>
            <span class="badge badge-status badge-status-active h-fit">
                <i class="fa-solid fa-check mr-1"></i>
                ${escapeHtml(
                    question.opcionesRespuesta[correctAnswerIndex].texto
                )}
            </span>
        </td>

        ${quizFormData.editable ? `
            <td>
                <div class="flex justify-center gap-1">
    
                    <button type="button"
                            class="btn btn-ghost btn-square btn-sm"
                            title="Editar pregunta"
                            data-edit-question
                            data-question-index="${index}">
                        <i class="fa-regular fa-pen-to-square"></i>
                    </button>
    
                    <button type="button"
                            class="btn btn-ghost btn-square btn-sm"
                            title="Eliminar pregunta"
                            data-delete-question
                            data-question-index="${index}">
                        <i class="fa-regular fa-trash-can"></i>
                    </button>
    
                </div>
            </td>
        `: ''}
    `;

    return row;
}

function escapeHtml(value) {
    const div = document.createElement('div');

    div.textContent = value ?? '';

    return div.innerHTML;
}

function updateQuestionsView() {
    const hasQuestions = questions.length > 0;

    questionsEmptyState?.classList.toggle(
        'hidden',
        hasQuestions
    );

    questionsList?.classList.toggle(
        'hidden',
        !hasQuestions
    );
}

function renderQuestions() {
    if (!questionsListBody) {
        return;
    }

    questionsListBody.innerHTML = '';

    questions.forEach((question, index) => {
        questionsListBody.appendChild(
            createQuestionRow(question, index)
        );
    });

    updateQuestionsView();
}

function refreshQuestions() {
    renderQuestions();
    syncQuestionsWithForm();
}

function removeQuestion(index) {
    if (index < 0 || index >= questions.length) {
        return;
    }

    questions.splice(index, 1);

    refreshQuestions();
}

// function getQuestionTypeLabel(type) {
//     const labels = {
//         SINGLE_CHOICE: 'Respuesta única',
//         TRUE_FALSE: 'Verdadero/Falso'
//     };

//     return labels[type] ?? type;
// }

function configureQuestionType() {

    if (!questionType) {
        return;
    }

    if (questionType.value === 'TRUE_FALSE') {
        configureTrueFalseQuestion();
        return;
    }

    configureSingleChoiceQuestion();
}

function configureSingleChoiceQuestion() {
    addAnswerButton?.classList.remove('hidden');

    answersContainer.innerHTML = '';

    for (let i = 0; i < QUESTIONS_RULES[questionType.value].minAnswer; i++) {
        answersContainer.appendChild(createAnswer());
    }

    updateAnswerLetters();
    updateAddAnswerBtn();
}

function configureTrueFalseQuestion() {
    addAnswerButton?.classList.add('hidden');

    answersContainer.innerHTML = '';

    answersContainer.appendChild(
        createFixedAnswer('Verdadero')
    );

    answersContainer.appendChild(
        createFixedAnswer('Falso')
    );

    updateAnswerLetters();
}

function createFixedAnswer(text, feedback = null) {
    const answer = document.createElement('div');

    answer.className = 'flex items-start gap-2 bg-base-200 rounded p-4';
    answer.dataset.answer = '';

    answer.innerHTML = `
        <div class="answer-letter flex items-center justify-center
                    w-8 h-8 shrink-0
                    rounded-md
                    bg-primary/10
                    text-primary
                    font-semibold text-sm">
            A
        </div>

        <div class="flex-1 min-w-0">
            <input type="text"
                class="input w-full"
                data-answer-text
                maxlength="255"
                value="${escapeHtml(text)}"
                readonly>
        
            <textarea 
                class="textarea textarea-sm w-full mt-2"
                data-answer-feedback
                maxlength="500" 
                placeholder="Retroalimentación opcional...">
                
                ${escapeHtml(feedback ?? '')}
            </textarea>
        </div>

        <label class="cursor-pointer"
               title="Marcar como respuesta correcta">

            <input type="radio"
                   name="question-correct-answer"
                   class="radio radio-primary"
                   data-answer-correct>

        </label>
    `;

    return answer;
}

function setQuestionDialogMode(mode) {
    questionDialog.dataset.mode = mode;

    if (mode === 'edit') {
        saveQuestionButton.innerHTML = `
            <i class="fa-regular fa-floppy-disk"></i>
            Guardar cambios
        `;

        return;
    }

    saveQuestionButton.innerHTML = `
        <i class="fa-solid fa-plus"></i>
        Añadir pregunta
    `;
}

function loadQuestionIntoDialog(question) {

    setQuestionDialogMode('edit');

    const questionText =
        document.getElementById('question-text');

    questionText.value = question.enunciado;

    questionType.value = question.tipoPregunta.codigo;

    answersContainer.innerHTML = '';

    if (question.tipoPregunta?.codigo === 'TRUE_FALSE') {

        question.opcionesRespuesta.forEach(opcion => {

            const answer =
                createFixedAnswer(opcion.texto, opcion.retroalimentacion);

            const correct =
                answer.querySelector('[data-answer-correct]');

            correct.checked = opcion.correcta;

            answersContainer.appendChild(answer);
        });

    } else {

        question.opcionesRespuesta.forEach(opcion => {

            const answer = createAnswer();

            answer.querySelector('[data-answer-text]').value =
                opcion.texto;

            answer.querySelector('[data-answer-feedback]').value =
                opcion.retroalimentacion ?? '';

            answer.querySelector('[data-answer-correct]').checked =
                opcion.correcta;

            answersContainer.appendChild(answer);
        });
    }

    updateAnswerLetters();
    updateAddAnswerBtn();
    clearQuestionErrors();
}

function syncQuestionsWithForm() {
    if (!questionsFormData) {
        return;
    }

    questionsFormData.innerHTML = '';

    questions.forEach((question, questionIndex) => {

        appendHiddenInput(
            `preguntas[${questionIndex}].enunciado`,
            question.enunciado
        );

        appendHiddenInput(
            `preguntas[${questionIndex}].tipoPreguntaCodigo`,
            question.tipoPregunta.codigo
        );

        question.opcionesRespuesta.forEach(
            (opcion, answerIndex) => {

                appendHiddenInput(
                    `preguntas[${questionIndex}].opcionesRespuesta[${answerIndex}].texto`,
                    opcion.texto
                );

                appendHiddenInput(
                    `preguntas[${questionIndex}].opcionesRespuesta[${answerIndex}].correcta`,
                    opcion.correcta
                );

                appendHiddenInput(
                    `preguntas[${questionIndex}].opcionesRespuesta[${answerIndex}].retroalimentacion`,
                    opcion.retroalimentacion
                );
            }
        );
    });
}

function appendHiddenInput(name, value) {
    const input = document.createElement('input');

    input.type = 'hidden';
    input.name = name;
    input.value = value;

    questionsFormData.appendChild(input);
}

// EVENTOS
addAnswerButton?.addEventListener('click', addAnswer);

answersContainer?.addEventListener('click', event => {
    const removeButton = event.target.closest('[data-remove-answer]');

    if (!removeButton) {
        return;
    }

    const answer = removeButton.closest('[data-answer]');

    if (answer) {
        removeAnswer(answer);
    }
});

saveQuestionButton?.addEventListener('click', () => {

    if (!validateQuestion()) {
        return;
    }

    const question = getQuestionFromDialog();
    const mode = questionDialog.dataset.mode;

    if (mode === 'edit') {
        const index = Number(questionDialog.dataset.questionIndex);

        if (index >= 0 && index < questions.length) {
            questions[index] = question;
        }

    } else{
        questions.push(question);
    }


    refreshQuestions();

    questionDialog.close();
});

questionsListBody?.addEventListener('click', event => {

    const editButton = event.target.closest('[data-edit-question]');

    if (editButton) {
        const index = Number(editButton.dataset.questionIndex);
        openEditQuestionDialog(index);
        return;
    }

    const deleteButton =
        event.target.closest('[data-delete-question]');

    if (deleteButton) {
        const index =
            Number(deleteButton.dataset.questionIndex);
    
        removeQuestion(index);
    }

});

questionType?.addEventListener('change', () => {
    configureQuestionType();
})

document.addEventListener('DOMContentLoaded', () => {
    renderQuestions();
    syncQuestionsWithForm();
});