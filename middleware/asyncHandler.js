// Wrapper to catch async and sync errors and pass them to the error middleware
const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve()
    .then(() => fn(req, res, next))
    .catch(next);
};

module.exports = { asyncHandler };
