x = scan('/Users/nandu/Downloads/recommender101/data/movielens/ml-1m/ratings.dat',what=list(integer(),integer(),numeric(),NULL),sep='|')[c(1,2,3)]
library('Matrix')
N = sparseMatrix(i=x[[1]],j=x[[2]],x=ifelse(x[[3]]<5,0,1))
object.size(N)
nnzero(N)
library('irlba')
S <- irlba(N, nu=10, nv=10)
#S$d
#S$u
#S$v
U <- S$u
s <- diag(S$d)
I <- U%*%s
I
#plot(S$d, main="Largest singular values")
